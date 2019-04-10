/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatregisteredcompanies.repositories

import java.time.Instant

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{Cursor, ReadConcern, ReadPreference}
import reactivemongo.api.indexes.{CollectionIndexesManager, Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, _}
import reactivemongo.core.nodeset.ProtocolMetadata
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONBatchCommands.FindAndModifyCommand
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber, VatRegisteredCompany}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final case class Wrapper(
  vatNumber: VatNumber,
  company: VatRegisteredCompany
)

object Wrapper {
  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue = {
      Json.toJson(BSONDateTime(o.toEpochMilli))
    }

    override def reads(json: JsValue): JsResult[Instant] = {
      json.validate[BSONDateTime] map { dt => Instant.ofEpochMilli(dt.value) }
    }
  }

  val format: Format[Wrapper] = Json.format[Wrapper]
}

@Singleton
class   VatRegisteredCompaniesRepository @Inject()(
  reactiveMongoComponent: ReactiveMongoComponent,
  bufferRepository: PayloadBufferRepository
)(implicit val executionContext: ExecutionContext, mat: Materializer) extends
  ReactiveRepository("vatregisteredcompanies", reactiveMongoComponent.mongoConnector.db, Wrapper.format) {

  private val im: CollectionIndexesManager = collection.indexesManager

  implicit val format: OFormat[Wrapper] = Json.format[Wrapper]

  val bulkSize: Int = ProtocolMetadata.Default.maxBulkSize - 1

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    Logger.info(s"inserting ${entries.length} entries")
    bulkInsert(entries).map(_ => (()))
  }

  private def streamingDelete(deletes: List[VatNumber], payload: PayloadWrapper) = {
    if (deletes.nonEmpty) {
      Logger.info(s"deleting ${deletes.length} records")
      val source = Source(deletes)
      val sink = Flow[VatNumber]
        .map(vrn =>
          collection.findAndRemove(Json.obj("vatNumber" -> vrn)).map {_.result[VatNumber]}
        ).to(Sink.onComplete{x =>
        Logger.info("End of deletion stream")
        bufferRepository.deleteOne(payload)
      })
      source.to(sink).run()
    } else {
      Logger.info("No deletes to process, cleaning buffer")
      bufferRepository.deleteOne(payload)
    }
    Future.successful((()))
  }

  private def deleteById(deletes: List[BSONValue]): Future[Unit] = {
    if(deletes.nonEmpty) {
      Logger.info(s"Deleting ${deletes.length} old entries")
      val source = Source(deletes)
      val sink = Flow[BSONValue]
        .map(_id =>
          collection.findAndRemove(Json.obj("_id" -> _id)).map {_.result[BSONValue]}
        ).to(Sink.onComplete { _ =>
        Logger.info("End of old entries deletion stream")
      })
      source.to(sink).run()
    }
    Future.successful((()))
  }

  private def findOld(n: Int): Future[List[BSONDocument]] = {
    import collection.BatchCommands.AggregationFramework.{Group, Limit, Match, MinField, SumAll}
    collection.aggregateWith[BSONDocument](allowDiskUse = true, readConcern = Some(ReadConcern.Local), readPreference = ReadPreference.nearest, batchSize = 1000.some) { _ =>
      (Group(JsString("$vatNumber"))( "count" -> SumAll, "oldest" -> MinField("_id")),
      List(
        Match(Json.obj("count" -> Json.obj("$gt" -> 1L))),
        Limit(n)
      ))
    }.collect[List](-1, Cursor.FailOnError[List[BSONDocument]]())
  }

  def deleteOld(n: Int): Future[Unit] = {
    findOld(n).flatMap(x => deleteById(x.flatMap(y => y.get("oldest"))))
  }

  private def wrap(payload: Payload): List[Wrapper] =
    payload.createsAndUpdates.map { company =>
      Wrapper(company.vatNumber, company)
    }

  def process(payload: PayloadWrapper): Future[Unit] = {
    for {
      a <- insert(wrap(payload.payload))
      b <- streamingDelete(payload.payload.deletes, payload)
    } yield (())
  }

  def lookup(target: String): Future[Option[LookupResponse]] = {
    collection
      .find(BSONDocument("vatNumber" -> target), Option.empty[JsObject])
      .sort(Json.obj("_id" -> -1))
      .cursor[Wrapper]()
      .collect[List](1, Cursor.FailOnError[List[Wrapper]]())
      .map {
        _.headOption.map(x => LookupResponse(x.company.some))
      }
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      name = "vatNumberIndex".some,
      key = Seq( "vatNumber" -> IndexType.Text),
      background = true,
      unique = false
    )
  )

  private val setIndexes: Future[Unit] = {
    for {
      list <- im.list()
    } yield list.filterNot(y => y.name === "vatNumberIndex".some || y.name === "_id_".some).foreach{ x =>
      im.drop(x.name.getOrElse(""))
    }
  }

  val getIndexes: Future[Unit] = {
    for {
      list <- im.list()
    } yield list.foreach { x =>
      Logger.info(s"Found index ${x.name}")
    }
  }

}

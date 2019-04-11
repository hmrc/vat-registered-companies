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
class VatRegisteredCompaniesRepository @Inject()(
  reactiveMongoComponent: ReactiveMongoComponent
)(implicit val executionContext: ExecutionContext, mat: Materializer) extends
  ReactiveRepository("vatregisteredcompanies", reactiveMongoComponent.mongoConnector.db, Wrapper.format) {

  private val im: CollectionIndexesManager = collection.indexesManager

  implicit val format: OFormat[Wrapper] = Json.format[Wrapper]

  val bulkSize: Int = ProtocolMetadata.Default.maxBulkSize - 1

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    Logger.info(s"inserting ${entries.length} entries")
    bulkInsert(entries).map(_ => (()))
  }

  private def streamingDelete(deletes: List[VatNumber]): Future[Unit] = Future {
    if (deletes.nonEmpty) {
      Logger.info(s"deleting ${deletes.length} records")
      val source = Source(deletes)
      val stage: RunnableGraph[NotUsed] = source.to(Sink.foreach[VatNumber] ( vrn =>
        collection.findAndRemove(Json.obj("vatNumber" -> vrn)).map(_.result[VatNumber])))
      stage.run()
    } else {
      ()
    }
  }

  private def deleteById(deletes: List[BSONValue]): Future[Unit] = {
    val it = deletes.sliding(bulkSize,bulkSize)

    def process(chunk: List[BSONValue]): Future[Unit] = {
      logger.info(s"deleting ${chunk.length} old entries")
      val deleteBuilder = collection.delete(false)
      val ds: Future[List[collection.DeleteCommand.DeleteElement]] =
        Future.sequence(
          chunk.map { id =>
            deleteBuilder.element(
              q = BSONDocument("_id" -> id),
              limit = None,
              collation = None)
          }
        )
      ds.flatMap { ops =>
        deleteBuilder.many(ops)
      }.map { multiBulkWriteResult =>
          multiBulkWriteResult.errmsg.foreach(e =>
            Logger.error(s"$e")
          )
      }
    }
    
    it.foldLeft(Future.successful(())){(a,b) => a flatMap {_ => process(b)}}
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

  def process(payload: Payload): Future[Unit] = {
    def wrap(payload: Payload): List[Wrapper] =
      payload.createsAndUpdates.map { company =>
        Wrapper(company.vatNumber, company)
      }

    insert(wrap(payload)) >> streamingDelete(payload.deletes)
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

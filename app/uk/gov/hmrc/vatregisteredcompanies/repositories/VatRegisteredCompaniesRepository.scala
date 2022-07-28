/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.implicits._
import com.mongodb.client.model.Collation

import javax.inject.{Inject, Named, Singleton}
import play.api.{Logger, Logging}
import play.api.libs.json._
import play.shaded.ahc.io.netty.util.concurrent.FastThreadLocal.removeAll
import org.mongodb.scala.bson.{BsonDocument, ObjectId}
import org.mongodb.scala.{DuplicateKeyException, MongoException, ReadConcern, ReadPreference, WriteConcern}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, FindOneAndDeleteOptions, IndexModel, IndexOptions, InsertOneOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber, VatRegisteredCompany}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.localDateTimeReads

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class Wrapper(
  vatNumber: VatNumber,
  company: VatRegisteredCompany
)

object Wrapper {
  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue = {
      Json.toJson(LocalDateTime.ofEpochSecond(o.toEpochMilli, 0, ZoneOffset.UTC))
    }

    override def reads(json: JsValue): JsResult[Instant] = {
      json.validate[LocalDateTime] map { dt => Instant.ofEpochMilli(dt.toEpochSecond(ZoneOffset.UTC)) }
    }
  }

  val format: Format[Wrapper] = Json.format[Wrapper]
}

@Singleton
class   VatRegisteredCompaniesRepository @Inject()(
  mongoComponent: MongoComponent,
  bufferRepository: PayloadBufferRepository,
  @Named("deletionThrottleElements") elements: Int,
  @Named("deletionThrottlePer") per: FiniteDuration
)(implicit val executionContext: ExecutionContext, mat: Materializer) extends
  PlayMongoRepository[Wrapper](
    mongoComponent = mongoComponent,
    collectionName = "vatregisteredcompanies",
    domainFormat = Wrapper.format,
    indexes = Seq(IndexModel(ascending("vatNumber"),
      IndexOptions().name("vatNumberIndexNew").unique(false).background(true)))) with Logging {


  def deleteAll(): Future[Unit] =
    collection.deleteMany(Filters.empty()).toFuture().map(_ => ())

  //private val im: CollectionIndexesManager = collection.indexesManager

  implicit val format: OFormat[Wrapper] = Json.format[Wrapper]

  //val bulkSize: Int = ProtocolMetadata.Default.maxBulkSize - 1

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    logger.info(s"inserting ${entries.length} entries")
    collection.insertMany(entries).headOption().map(_ => (()))
    //bulkInsert(entries).map(_ => (()))
  }

  private def streamingDelete(deletes: List[VatNumber], payload: PayloadWrapper) = {
    if (deletes.nonEmpty) {
      logger.info(s"deleting ${deletes.length} records")
      val source = Source(deletes)
      val options = FindOneAndDeleteOptions()
      // See https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/throttle.html
      // we could work out how many records we can safely delete given the size of the collection
      // and pass that in as a costCalculation
      val sink = Flow[VatNumber]
        .throttle(elements, per)
        .map(vrn => {
          //collection.findAndRemove(Json.obj("vatNumber" -> vrn), None, None, writeConcern = WriteConcern.Default, None, None, Seq.empty).map {
          collection.findOneAndDelete(BsonDocument("vatNumber" -> vrn)).headOption().map {
            _ => ()
          }
        }).to(Sink.onComplete{x =>
          x match {
            case Failure(e) =>
              logger.error(s"Unable to process streaming deletes at $elements per ${per._1} ${per._2}: ${e.getMessage}")
            case Success(_) =>
              bufferRepository.deleteOne(payload)
              logger.info("Processed streaming deletes")
          }
        logger.info(s"End of deletion stream")
        })
      source.to(sink).run()
    } else {
      logger.info("No deletes to process, cleaning buffer")
      bufferRepository.deleteOne(payload)
    }
    Future.successful((()))
  }

  private def deleteById(deletes: List[Wrapper]): Future[Unit] = {
    if(deletes.nonEmpty) {
      logger.info(s"Deleting ${deletes.length} old entries")
      val source = Source(deletes)
      val sink = Flow[Wrapper]
        .map(_id =>
          //collection.findAndRemove(Json.obj("_id" -> _id), None, None, writeConcern = WriteConcern.Default, None, None, Seq.empty).map {_.result[BSONValue]}
          collection.findOneAndDelete(Filters.equal("_id", _id))
            .map{_=>
              logger.info(s"Releasing lock ${_id}")
              ()
            })
        .to(Sink.onComplete { _ =>
        logger.info("End of old entries deletion stream")
      })
      source.to(sink).run()
    }
    Future.successful((()))
  }

  private def findOld(n: Int): Future[List[Wrapper]] = {
    //import collection .BatchCommands.AggregationFramework.{Group, Limit, Match, MinField, SumAll}

    collection.aggregate[Wrapper](Seq(equal("allowDiskUse", true), equal("readConcern", Some(ReadConcern.DEFAULT)),
      equal("readPreference", ReadPreference.nearest), equal("batchSize", 1000.some))) { _ =>
      //equal("_id", 1)allowDiskUse = true, readConcern = Some(ReadConcern.Local), readPreference = ReadPreference.nearest, batchSize = 1000.some) { _ =>
      (Group(JsString("$vatNumber"))( "count" -> SumAll, "oldest" -> MinField("_id")),
      List(
        Match(Json.obj("count" -> Json.obj("$gt" -> 1L))),
        Limit(n)
      ))
    }.collect[List](-1, List[Wrapper]())
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
      .find(BsonDocument("vatNumber" -> target))
      .sort(equal("_id", 1))
      .headOption()
      .map(x => {
        x match {
          case Some(y) => Some(LookupResponse(target = Some(y.company)))
          case None => None
        }
      })
      }

  /*override def indexes: Seq[Index] = Seq(
    Index(
      name = "vatNumberIndexNew".some,
      key = Seq( "vatNumber" -> IndexType.Ascending),
      background = true,
      unique = false
    )
  )

  val getIndexes: Future[Unit] = {
    for {
      list <- im.list()
    } yield list.foreach { x =>
      logger.info(s"Found index ${x.name}")
    }
  }

   */

}

/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.stream.Materializer
import org.mongodb.scala.bson.{BsonDocument, BsonValue, ObjectId}
import org.mongodb.scala.model.Aggregates.{group, limit, project}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJavatimeFormats}
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber, VatRegisteredCompany}

import java.time.Instant
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}


final case class Wrapper(
  vatNumber: VatNumber,
  company: VatRegisteredCompany
)

object Wrapper {
    implicit val localDateTimeFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val formats: OFormat[Wrapper] = Json.format
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
    domainFormat = Wrapper.formats,
    indexes = Seq(IndexModel(ascending("vatNumber"),
      IndexOptions().name("vatNumberIndexNew").unique(false).background(true)))) with Logging {

  def deleteOld(n: Int): Future[Unit] = {
    for {
      vatRegCompId <- findOld(n)
      _ <- deleteById(vatRegCompId)
    } yield (): Unit
  }

  case class VatRegCompId(oldest: ObjectId)

    implicit val objectIdFormat = MongoFormats.objectIdFormat
    implicit val formatVatRegCompId = Json.format[VatRegCompId]

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    if(entries.nonEmpty) {
      logger.info(s"inserting ${entries.length} entries")
      collection.insertMany(entries).headOption().map(_ => ())
    } else {
      Future.successful(())
    }
  }

  private def streamingDelete(deletes: List[VatNumber], payload: PayloadWrapper): Future[Unit] = {
    deletes match {
      case vrn :: tail =>
        collection.deleteMany(Filters.equal("vatNumber", vrn))
          .toFuture()
          .flatMap {_ =>
            if(tail.nonEmpty) {
              streamingDelete(tail, payload)
            }
            else {
              bufferRepository.deleteOne(payload).map { _ =>
                logger.info("Processed streaming deletes")
              }
            }
          }
      case Nil => logger.info("No deletes to process, cleaning buffer")
        bufferRepository.deleteOne(payload)
    }
  }

  private def deleteById(deletes: Seq[VatRegCompId]): Future[Unit] = {
    deletes match {
      case vrcid :: tail =>
        collection.deleteOne(Filters.equal("_id", vrcid.oldest))
          .toFuture()
          .flatMap {_ =>
            if(tail.nonEmpty) {
              deleteById(tail)
            }
            else {
              Future.successful(logger.info("End of old entries deletion stream"))
            }
          }
      case Nil => Future.successful(logger.info("No old entries to delete"))
    }
  }

  private def wrap(payload: Payload): List[Wrapper] =
    payload.createsAndUpdates.map { company =>
      Wrapper(company.vatNumber, company)
    }

  def process(payload: PayloadWrapper): Future[Unit] = {
    for {
      a <- insert(wrap(payload.payload))
      b <- streamingDelete(payload.payload.deletes, payload)
    } yield ()
  }

  def lookup(target: String): Future[Option[LookupResponse]] = {
    collection
      .find(BsonDocument("vatNumber" -> target))
      .sort(Sorts.descending("_id"))
      .headOption()
      .map(x => {
        x match {
          case Some(y) => Some(LookupResponse(target = Some(y.company)))
          case None => None
        }
      })
  }

  private def findOld(n: Int): Future[Seq[VatRegCompId]] = {
    collection.aggregate[BsonValue](Seq(
      group("$vatNumber", Accumulators.sum("count", 1), Accumulators.min(
        "oldest", "$_id")),
      Aggregates.filter(Filters.gt("count", 1)),
      limit(n),
      project(include("oldest"))
    )).allowDiskUse(true).toFuture()
      .map(res => {
        res.map(Codecs.fromBson[VatRegCompId](_))
      })
  }
}

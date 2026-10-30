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

import org.mongodb.scala.bson.{BsonDocument, BsonValue, ObjectId}
import org.mongodb.scala.model.Aggregates.{group, limit, project}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.*
import play.api.{Configuration, Logging}
import play.api.libs.json.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.{MongoFormats, MongoJavatimeFormats}
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber, VatRegisteredCompany}

import java.time.Instant
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.{MongoServerException, MongoWriteException, ObservableFuture, SingleObservableFuture}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Format.GenericFormat
import uk.gov.hmrc.http.HeaderCarrier

final case class Wrapper(
                          vatNumber: VatNumber,
                          company: VatRegisteredCompany
                        )

object Wrapper {
  implicit val localDateTimeFormats: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val formats: OFormat[Wrapper] = Json.format[Wrapper]
}

@Singleton
class VatRegisteredCompaniesRepository @Inject()(
                                                  mongoComponent: MongoComponent,
                                                  bufferRepository: PayloadBufferRepository,
                                                  @Named("deletionThrottleElements") elements: Int,
                                                  @Named("deletionThrottlePer") per: FiniteDuration,
                                                  val configuration: Configuration
                                                )(implicit val executionContext: ExecutionContext) extends
  PlayMongoRepository[Wrapper](
    mongoComponent = mongoComponent,
    collectionName = "vatregisteredcompanies",
    domainFormat = Wrapper.formats,
    indexes = Seq(
      IndexModel(
        ascending("vatNumber", "_id"),
        IndexOptions().name("vatNumber_id_compound_idx").background(true)
      )
    )
  ) with Logging {

  private val prepareUniqueEnabled : Boolean = configuration.getOptional[Boolean]("microservice.services.vatRegisteredCompanies.prepare-unique.enabled").getOrElse(false)

  def applyPrepareUnique() : Future[Unit] = {
    if (!prepareUniqueEnabled) {
      logger.info("prepare Unique Disabled: vatNumber index not created")
      Future.unit
    } else {
      (for {
        _ <- collection.createIndex(
          Indexes.ascending("vatNumber"),
          IndexOptions().name("vatNumber_prepare_unique_idx").background(true)
        ).toFuture()
        _ <- mongoComponent.database.runCommand(
          BsonDocument(
            "collMod" -> "vatregisteredcompanies",
            "index" -> BsonDocument(
              "name" -> "vatNumber_prepare_unique_idx",
              "prepareUnique" -> true
            )
          )
        ).toFuture()
      } yield logger.info(s"vatNumber index status, prepareUniqueIndexFlag= $prepareUniqueEnabled")).recover {
        case e => logger.warn(s"failed to apply prepareUnique on vatNumber index: ${e.getMessage}", e)
      }
    }
  }

  private val prepareUniqueInit: Future[Unit] = applyPrepareUnique()

  def awaitPrepareUnique: Future[Unit] = prepareUniqueInit
  
  def deleteOld(n: Int): Future[Unit] = {
    for {
      vatRegCompId <- findOld(n)
      _ = if (vatRegCompId.isEmpty)
        logger.info(s"Reconciliation complete: no duplicate vatNumbers remaining")
      else
        logger.info(s"Reconciliation in progress: ${vatRegCompId.size} duplicate groups found")
      _ <- deleteById(vatRegCompId)
    } yield ()
  }

  case class VatRegCompId(oldest: ObjectId)

  object VatRegCompId {
    implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
    implicit val formatVatRegCompId: OFormat[VatRegCompId] = Json.format[VatRegCompId]
  }

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    val latest = entries.reverse.distinctBy(_.vatNumber).reverse
    if (latest.isEmpty) Future.unit
    else {
      logger.info(s"upserting ${entries.length} entries")

      latest.grouped(50).foldLeft(Future.unit) { (previousBatch, batch) =>
        previousBatch.flatMap { _ => upsertBatch(batch)
        }
      }
    }
  }

  private def upsertBatch(batch : List[Wrapper]) : Future[Unit] =
    Future.traverse(batch){ entry =>
      upsertOne(entry).map(_ => Right(entry.vatNumber)).recover{case e => Left((entry.vatNumber,e))}
      }.flatMap{ results=>
      val failures = results.collect{ case Left(f) => f }
      if (failures.isEmpty) Future.unit
      else {
        logger.error(s"Upsert batch failed: ${failures.size} of ${batch.size} writes failed", failures.head._2)
        Future.failed(failures.head._2)
      }
    }


  private def upsertOne(entry:Wrapper): Future[Unit] = {

        val filter = Filters.equal ("vatNumber", entry.vatNumber)

        val update = Updates.combine(
          Updates.set("vatNumber", entry.vatNumber),
          Updates.set("company", Codecs.toBson(entry.company))
        )
        collection.findOneAndUpdate(
          filter,
          update,
          FindOneAndUpdateOptions().sort(Sorts.descending("_id")).upsert(true)
          ).headOption().map(_=> ())
    }


  private def streamingDelete(deletes: List[VatNumber], payload: PayloadWrapper): Future[Unit] = {
    deletes match {
      case vrn :: tail =>
        collection.deleteMany(Filters.equal("vatNumber", vrn))
          .toFuture()
          .flatMap { _ =>
            if (tail.nonEmpty) {
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

  private def deleteById(deletes: Seq[VatRegCompId]): Future[Unit] =
    deletes match {
      case Nil => Future.successful(logger.info("No old entries to delete"))
      case vrcid :: tail =>
        collection.deleteOne(Filters.equal("_id", vrcid.oldest))
          .toFuture()
          .flatMap { _ =>
            if (tail.nonEmpty) {
              deleteById(tail)
            }
            else {
              Future.successful(logger.info("End of old entries deletion stream"))
            }
          }
    }

  private def wrap(payload: Payload): List[Wrapper] =
    payload.createsAndUpdates.map { company =>
      Wrapper(company.vatNumber, company)
    }

  def process(payload: PayloadWrapper): Future[Unit] = {
    for {
      _ <- insert(wrap(payload.payload))
      _ <- streamingDelete(payload.payload.deletes, payload)
    } yield ()
  }

  def lookup(target: String)(implicit hc: HeaderCarrier): Future[Option[LookupResponse]] = {
    val startTime = System.currentTimeMillis()
    val requestId = hc.requestId.map(_.value).getOrElse("-")
    
    logger.info(s"VatRegisteredCompanies mongo lookup start  requestId=$requestId")

    collection
      .find(BsonDocument("vatNumber" -> target))
      .sort(Sorts.descending("_id"))
      .headOption()
      .map {
        case Some(y) => 
          logger.info(s"VatRegisteredCompanies mongo lookup found requestId=$requestId durationMs=${System.currentTimeMillis() - startTime}")
          Some(LookupResponse(target = Some(y.company)))
        case None =>
          logger.info(s"VatRegisteredCompanies mongo lookup not found requestId=$requestId durationMs=${System.currentTimeMillis() - startTime}")
          None
      }
  }

  private def findOld(n: Int): Future[Seq[VatRegCompId]] = {
    collection.aggregate[BsonValue](Seq(
        Aggregates.sort(Sorts.ascending("vatNumber", "_id")),
        Aggregates.group("$vatNumber", Accumulators.sum("count", 1), Accumulators.min(
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

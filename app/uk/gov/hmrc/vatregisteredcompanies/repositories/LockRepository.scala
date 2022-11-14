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

import java.time.{Instant, LocalDateTime, ZoneOffset}
import com.google.inject.ImplementedBy
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{DuplicateKeyException, MongoException, ReadPreference, WriteConcern}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndDeleteOptions, IndexModel, IndexOptions}

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

final case class Lock(
  _id: Int,
  lastUpdated: LocalDateTime = LocalDateTime.now
)

trait MongoDateTimeFormats {

  implicit val localDateTimeRead: Reads[LocalDateTime] =
    (__ \ "$date").read[Long].map {
      millis =>
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
    }

  implicit val localDateTimeWrite: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    def writes(dateTime: LocalDateTime): JsValue = Json.obj(
      "$date" -> dateTime.atZone(ZoneOffset.UTC).toInstant.toEpochMilli
    )
  }
}

object Lock extends MongoDateTimeFormats {

  implicit val formats: OFormat[Lock] = Json.format
}

@Singleton
class DefaultLockRepository @Inject()(
  mongoComponent: MongoComponent,
  val runModeConfiguration: Configuration
)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Lock](
    mongoComponent = mongoComponent,
    collectionName = "locks",
    domainFormat = Lock.formats,
    indexes = Seq(IndexModel(ascending("lastUpdated"),
      IndexOptions().name("locks-index").expireAfter ((60 * 30).toLong, TimeUnit.SECONDS) .unique(false).sparse(false)))) with LockRepository with Logging {

  private lazy val documentExistsErrorCode = Some(11000)

  private val cacheTtl = 60 * 30 // TODO configure

  /*private val index = Index(
    key     = Seq("lastUpdated" -> IndexType.Ascending),
    name    = Some("locks-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  override def indexes: Seq[Index] = Seq(index)*/

  val ttl = runModeConfiguration.getOptional[Int]("microservice.services.lock.ttl.minutes").getOrElse(10)

  override def lock(id: Int): Future[Boolean] = {
    collection.insertOne(Lock(id)).toFuture().map{_ =>
      logger.info(s"Locking with $id")
      true
    }.recover {
        case e: DuplicateKeyException =>
          getLock(id).map {o =>
            o.map {t =>
              if (t.lastUpdated.isBefore(LocalDateTime.now.minusMinutes(ttl))) {
                release(id)
              }
            }
          }
          logger.info(s"Unable to lock with $id")
          false
        case e =>
          logger.info(s"An exception has occurred. Unable to lock with $id")
          false
      }
  }

  override def release(id: Int): Future[Unit] = {
    //val options =  FindOneAndDeleteOptions().sort(BsonDocument("ascending" -> g))

    collection.findOneAndDelete(BsonDocument("_id" -> id))
      .headOption()
            //findAndDelete( WriteConcern.ACKNOWLEDGED, None, None, Seq.empty)
      .map{_=>
        logger.info(s"Releasing lock $id")
        ()
      }.fallbackTo(Future.successful(()))
  }

  override def isLocked(id: Int): Future[Boolean] =
    collection.find[Lock](equal("_id", id))
      .headOption()
      .map(_.isDefined)

  def getLock(id: Int): Future[Option[Lock]] =
    collection.find[Lock](equal("_id", id))
      .headOption()

}

@ImplementedBy(classOf[DefaultLockRepository])
trait LockRepository {
  def lock(id: Int): Future[Boolean]
  def release(id: Int): Future[Unit]
  def isLocked(id: Int): Future[Boolean]
}

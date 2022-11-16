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
import org.mongodb.scala.{DuplicateKeyException, MongoException, MongoWriteException, ReadPreference, WriteConcern}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndDeleteOptions, IndexModel, IndexOptions}

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

final case class Lock(
  _id: Int,
  lastUpdated: LocalDateTime = LocalDateTime.now
)

object Lock {

  implicit val localDateTimeFormats: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
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
    println("@@@@@@@@@@@@@@@@@@@@@@@@@ start of lock method @@@@@@@@@@@@@@@@@@@@@@")
    collection.insertOne(Lock(id)).toFuture().map{_ =>
      logger.info(s"Locking with $id")
      true
    }.recoverWith {
        case e: MongoWriteException if e.getError.getCode == 11000 =>
          logger.info(s"Unable to lock with $id")
          getLock(id).flatMap {
            case Some(lock) if lock.lastUpdated.isBefore(LocalDateTime.now.minusMinutes(ttl)) =>
              release(id).map(_ => false)
            case _ => Future.successful(false)
          }
        case e =>
          logger.info(s"An exception has occurred. Unable to lock with $id")
          Future.successful(false)
      }
  }

  override def release(id: Int): Future[Unit] = {
    //val options =  FindOneAndDeleteOptions().sort(BsonDocument("ascending" -> g))

    println("release method called")
    collection.findOneAndDelete(BsonDocument("_id" -> id))
      .headOption()
            //findAndDelete( WriteConcern.ACKNOWLEDGED, None, None, Seq.empty)
      .map{_=>
        logger.info(s"Releasing lock $id")
        ()
      }.fallbackTo(Future.successful(()))
  }

  override def isLocked(id: Int): Future[Boolean] = {
    println("*****************Starting isLocked method - searching for a lock**********************")
    collection.find[Lock](equal("_id", id))
      .headOption()
      .map(_.isDefined)
  }

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

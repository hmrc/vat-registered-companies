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

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers.{JsObjectDocumentWriter => _, _}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

final case class Lock(
  pw_id: BSONObjectID,
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
  reactiveMongoComponent: ReactiveMongoComponent,
  config: Configuration
)(implicit ec: ExecutionContext)
  extends ReactiveRepository(
    "locks",
    reactiveMongoComponent.mongoConnector.db,
    PayloadWrapper.format) with LockRepository {

  private lazy val documentExistsErrorCode = Some(11000)

  private val cacheTtl = 60 * 30 // TODO configure

  private val index = Index(
    key     = Seq("lastUpdated" -> IndexType.Ascending),
    name    = Some("locks-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  override def indexes: Seq[Index] = Seq(index)

  override def lock(id: BSONObjectID): Future[Boolean] = {
    collection.insert(Lock(id)).map{x =>
      logger.info(s"Locking with $x")
      true
    }.recover {
      case e: LastError if e.code == documentExistsErrorCode =>
        false
      case e: LastError => {
        false
      }
    }
  }

  override def release(id: BSONObjectID): Future[Unit] =
    collection.findAndRemove(BSONDocument("pw_id" -> id))
      .map(_=> ()).fallbackTo(Future.successful(()))

  override def isLocked(id: BSONObjectID): Future[Boolean] =
    collection.find(BSONDocument("pw_id" -> id),None)
      .one[Lock].map(_.isDefined)

}

@ImplementedBy(classOf[DefaultLockRepository])
trait LockRepository {
  def lock(id: BSONObjectID): Future[Boolean]
  def release(id: BSONObjectID): Future[Unit]
  def isLocked(id: BSONObjectID): Future[Boolean]
}

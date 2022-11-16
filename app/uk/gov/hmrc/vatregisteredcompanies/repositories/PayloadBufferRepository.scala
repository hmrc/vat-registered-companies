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

import org.bson.types.ObjectId

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json._
import org.mongodb.scala.model.{Filters, Sorts}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.vatregisteredcompanies.models.Payload

import scala.concurrent.{ExecutionContext, Future}

final case class PayloadWrapper (
  _id: ObjectId = ObjectId.get(),
  payload: Payload
)

object PayloadWrapper {
  implicit  val format: Format[PayloadWrapper] = Json.format[PayloadWrapper]
}

@Singleton
class   PayloadBufferRepository@Inject()(
  mongoComponent: MongoComponent
)(implicit val executionContext: ExecutionContext)
  extends PlayMongoRepository[PayloadWrapper](
    mongoComponent = mongoComponent,
    collectionName = "vatregisteredcompaniesbuffer",
    domainFormat = PayloadWrapper.format,
    indexes = Seq()) with Logging {

  def deleteAll(): Future[Unit] =
    collection.deleteMany(Filters.empty()).toFuture().map(_ => ())

  def insert(payload: Payload): Future[Unit] =
    collection.insertOne(PayloadWrapper(payload = payload)).toFuture().map(_ => ())

  def list: Future[List[PayloadWrapper]] =
    collection.find(Filters.empty()).toFuture().map(_.toList)

  private def getOne: Future[List[PayloadWrapper]] = {
    collection
      .find(Filters.empty())
      .sort(Sorts.ascending("_id"))
      .toFuture()
      .map(_.toList)
  }

  def one: Future[Option[PayloadWrapper]] = getOne.map(_.headOption)

  def deleteOne(payload: PayloadWrapper): Future[Unit] = {
    collection.findOneAndDelete(Filters.equal("_id", payload._id))
      .headOption()
      .map{_=>
        logger.info(s"Releasing lock $payload._id")
        ()
      }.fallbackTo(Future.successful(()))
  }
}

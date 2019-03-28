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

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{QueryOpts, Cursor}
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatregisteredcompanies.models.Payload

import scala.concurrent.{ExecutionContext, Future}

final case class PayloadWrapper(
  _id: BSONObjectID = BSONObjectID.generate,
  payload: Payload
)

object PayloadWrapper {
  val format: Format[PayloadWrapper] = Json.format[PayloadWrapper]
}

@Singleton
class   PayloadBufferRepository@Inject()(reactiveMongoComponent: ReactiveMongoComponent)(implicit val executionContext: ExecutionContext)
  extends ReactiveRepository("vatregisteredcompaniesbuffer", reactiveMongoComponent.mongoConnector.db, PayloadWrapper.format) {

  implicit val format: OFormat[PayloadWrapper] = Json.format[PayloadWrapper]

  def insert(payload: Payload): Future[Unit] =
    collection.insert(PayloadWrapper(payload = payload)).map(_ => ())

  def list: Future[List[PayloadWrapper]] =
    findAll()

  def getOne: Future[List[PayloadWrapper]] = {
    val query = BSONDocument()
    collection
      .find(query, Option.empty[JsObject])
      .sort(Json.obj("_id" -> 1))
      .cursor[PayloadWrapper]()
      .collect[List](1, Cursor.FailOnError[List[PayloadWrapper]]())
  }

  def one: Future[Option[PayloadWrapper]] = getOne.map(_.headOption)


  def deleteMany(payloadWrapperList: List[PayloadWrapper]): Future[Unit] = {
    val ids: Seq[BSONObjectID] = payloadWrapperList.map(_._id)
    remove("_id" -> BSONDocument("$in" -> ids)).map { _ => (()) }
  }

  def deleteOne(payload: PayloadWrapper): Future[Unit] = {
    remove("_id" -> payload._id).map { _ => (())}
  }

}

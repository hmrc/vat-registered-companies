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

package uk.gov.hmrc.vatregisteredcompanies.helpers

import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.json._
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.api.Cursor
import uk.gov.hmrc.vatregisteredcompanies.models.{Payload, VatRegisteredCompany}
import uk.gov.hmrc.vatregisteredcompanies.repositories.{PayloadBufferRepository, PayloadWrapper}
import scala.concurrent.Future

trait PayloadBufferDatabaseOperations {

  self: IntegrationSpecBase =>

  val payloadBufferRepository: PayloadBufferRepository
  def createPayloadWrapper(payload: Payload): PayloadWrapper = {
    val _id: BSONObjectID = BSONObjectID.generate()
    val payloadWrapper = PayloadWrapper(_id, payload)
    payloadWrapper
  }
  def insertOneBuffer(payload: Payload): Boolean = {

      val payloadWrapper = createPayloadWrapper(payload)
    await(
      payloadBufferRepository.insert(payloadWrapper).map(_.ok)
    )
  }

  def listBuffer: Future[List[PayloadWrapper]] =
      payloadBufferRepository.findAll()

  def bufferTotalCount: Int = {
    await(payloadBufferRepository.count)
  }

  def getOneBuffer: Future[List[PayloadWrapper]] = {
    val query = BSONDocument()
    await(
      payloadBufferRepository
        .collection
        .find(query, Option.empty[JsObject])
        .sort(Json.obj("_id" -> 1))
        .cursor[PayloadWrapper]()
        .collect[List](1, Cursor.FailOnError[List[PayloadWrapper]]())
    )
  }

  def deleteOneBuffer(payload: PayloadWrapper): Future[Unit] = {
        payloadBufferRepository
          .remove("_id" -> payload._id).map { _ => (()) }
  }

}

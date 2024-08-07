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

package uk.gov.hmrc.vatregisteredcompanies.helpers

import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.vatregisteredcompanies.models.Payload
import uk.gov.hmrc.vatregisteredcompanies.repositories.{PayloadBufferRepository, PayloadWrapper}

import scala.concurrent.Future

trait PayloadBufferDatabaseOperations {

  self: IntegrationSpecBase =>

  val payloadBufferRepository: PayloadBufferRepository
  def createPayloadWrapper(payload: Payload): PayloadWrapper = {
    val _id: ObjectId = ObjectId.get()
    val payloadWrapper = PayloadWrapper(_id, payload)
    payloadWrapper
  }
  def insertOneBuffer(payload: Payload): Unit = {
      val payloadWrapper = createPayloadWrapper(payload)
      await(payloadBufferRepository.collection.insertOne(payloadWrapper).toFuture())
  }

  def listBuffer: Future[List[PayloadWrapper]] =
      payloadBufferRepository.list

  def bufferTotalCount: Long = {
   await(payloadBufferRepository.collection.countDocuments().toFuture())
  }

  def deleteOneBuffer(payload: Payload): Unit = {
    val payloadWrapper = createPayloadWrapper(payload)
    await(payloadBufferRepository.deleteOne(payloadWrapper))
  }
  def deleteAllBuffer(): Unit = {
      payloadBufferRepository.collection.deleteMany(Filters.empty()).toFuture()
  }

}

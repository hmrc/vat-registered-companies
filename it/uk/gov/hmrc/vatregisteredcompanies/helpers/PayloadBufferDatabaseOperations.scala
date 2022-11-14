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

import org.mongodb.scala.bson.BsonObjectId
import uk.gov.hmrc.vatregisteredcompanies.models.Payload
import uk.gov.hmrc.vatregisteredcompanies.repositories.{PayloadBufferRepository, PayloadWrapper}
import scala.concurrent.Future

trait PayloadBufferDatabaseOperations {

  self: IntegrationSpecBase =>

  val payloadBufferRepository: PayloadBufferRepository
  def createPayloadWrapper(payload: Payload): PayloadWrapper = {
    val _id: BsonObjectId = BsonObjectId()
    val payloadWrapper = PayloadWrapper(_id, payload)
    payloadWrapper
  }
  def insertOneBuffer(payload: Payload): Unit = {
      val payloadWrapper = createPayloadWrapper(payload)
      payloadBufferRepository.collection.insertOne(payloadWrapper)
  }

  def listBuffer: Future[List[PayloadWrapper]] =
      payloadBufferRepository.list

  def bufferTotalCount: Int = {
    await(payloadBufferRepository.count)
  }

  def deleteOneBuffer(payload: Payload): Future[Unit] = {
    val payloadWrapper = createPayloadWrapper(payload)
    payloadBufferRepository.deleteOne(payloadWrapper)
  }
  def deleteAllBuffer: Unit = {
      payloadBufferRepository.collection.drop()
  }

}

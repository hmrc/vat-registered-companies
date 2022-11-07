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

package uk.gov.hmrc.vatregisteredcompanies.repository

import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData._
import uk.gov.hmrc.vatregisteredcompanies.repositories.PayloadWrapper

class PayloadBufferRepositoryISpec extends IntegrationSpecBase {
  override def beforeEach(): Unit = {
    deleteAllBuffer
  }

  "Method: deleteAll" when {
    "there are no records in the database" should {
      "Have no records" in {
        bufferTotalCount shouldBe 0
        val res = payloadBufferRepository.deleteAll()

        whenReady(res) { result =>
          result shouldBe ((): Unit)
          bufferTotalCount shouldBe 0
        }
      }
    }

    "there is 1 record in the database" should {
      "Have one record" in {
        insertOneBuffer(testPayloadCreateAndUpdates1)
        bufferTotalCount shouldBe 1
        val res = payloadBufferRepository.deleteAll()

        whenReady(res) { result =>
          result shouldBe ((): Unit)
          bufferTotalCount shouldBe 0
        }
      }
    }

    "there are multiple records in the database" should {
      "Have two records" in {
        insertOneBuffer(testPayloadCreateAndUpdates)
        insertOneBuffer(testPayloadCreateAndUpdates1)
        bufferTotalCount shouldBe 2
        val res = payloadBufferRepository.deleteAll()

        whenReady(res) { result =>
          result shouldBe ((): Unit)
          bufferTotalCount shouldBe 0
        }
      }
    }

    "Method: insert" when {
      "there are no records in the database" should {
        "Add one record" in {
          val res = payloadBufferRepository.insert(testPayloadCreateAndUpdates1)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            bufferTotalCount shouldBe 1
          }
        }
      }

      "there are multiple records in the database" should {
        "Increase the database by one record" in {
          insertOneBuffer(testPayloadCreateAndUpdates1)
          insertOneBuffer(testPayloadCreateAndUpdates)
          bufferTotalCount shouldBe 2

          val res = payloadBufferRepository.insert(testPayloadCreateAndUpdates1)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            bufferTotalCount shouldBe 3
          }
        }
      }

      "there are multiple records in the database" should {
        "Increase the database by two records" in {
          insertOneBuffer(testPayloadCreateAndUpdates1)
          insertOneBuffer(testPayloadCreateAndUpdates)
          bufferTotalCount shouldBe 2

          val res = {
            payloadBufferRepository.insert(testPayloadCreateAndUpdates1)
            payloadBufferRepository.insert(testPayloadCreateAndUpdates)
          }

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            bufferTotalCount shouldBe 4
          }
        }
      }
    }

    "Method: list" when {

      "there are no records in the database" should {
        "Have no records" in {
          val res = {
            payloadBufferRepository.list
          }

          whenReady(res) { result =>
            result shouldBe empty
            bufferTotalCount shouldBe 0
          }

        }
      }

      "there is 1 record in the database" should {
        "Have one record" in {

          insertOneBuffer(testPayloadCreateAndUpdates)
          val res = {
            payloadBufferRepository.list
          }

          whenReady(res) { result =>
            result.map(_.payload) shouldBe List(testPayloadCreateAndUpdates)
            bufferTotalCount shouldBe 1
          }
        }
      }

      "there are multiple records in the database" should {
        "Have three records" in {
          insertOneBuffer(testPayloadCreateAndUpdates1)
          insertOneBuffer(testPayloadCreateAndUpdates)
          insertOneBuffer(testPayloadCreateAndUpdates1)
          val res = {
            payloadBufferRepository.list
          }

          whenReady(res) { result =>

            result.head.payload shouldBe testPayloadCreateAndUpdates1
            bufferTotalCount shouldBe 3
          }
        }
      }
    }

<<<<<<< HEAD
//      "Method: getOne" when {
//        "there are no records in the database" should {
//          "Have no records" in {
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there is 1 record in the database" should {
//          "Have one record" in {
//            insertOne(getVatRegCompany(testVatNo1))
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there are multiple records in the database" should {
//          "Have three records" in {
//            insertMany(List(getVatRegCompany(testVatNo1), getVatRegCompany(testVatNo2), getVatRegCompany(testVatNo3)))
//            bufferTotalCount shouldBe 0
//          }
//        }
//      }
//      "Method: deleteOne" when {
//        "there are no records in the database" should {
//          "Have no records" in {
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there is 1 record in the database" should {
//          "Have one record" in {
//            insertOne(getVatRegCompany(testVatNo1))
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there are multiple records in the database" should {
//          "Have three records" in {
//            insertMany(List(getVatRegCompany(testVatNo1), getVatRegCompany(testVatNo2), getVatRegCompany(testVatNo3)))
//            bufferTotalCount shouldBe 0
//          }
////        }
//      }
//    }
//      "Method: getOne" when {
//        "there are no records in the database" should {
//          "Have no records" in {
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there is 1 record in the database" should {
//          "Have one record" in {
//            insertOne(getVatRegCompany(testVatNo1))
//            bufferTotalCount shouldBe 0
//          }
//        }
//
//        "there are multiple records in the database" should {
//          "Have three records" in {
//            insertMany(List(getVatRegCompany(testVatNo1), getVatRegCompany(testVatNo2), getVatRegCompany(testVatNo3)))
//            bufferTotalCount shouldBe 0
//          }
//        }
//      }
      "Method: deleteOne" when {
        "there are no records in the database" should {
          "Have no records" in {
            bufferTotalCount shouldBe 0
            val currentRecordsList = {
              payloadBufferRepository.list
            }
            val firstRecord = whenReady(currentRecordsList) { first => first.head.payload }
            val res = payloadBufferRepository.deleteOne(createPayloadWrapper(testPayloadDeletes))

            whenReady(res) { result =>
              result shouldBe ((): Unit)
              bufferTotalCount shouldBe 0
            }
        }

        "there is 1 record in the database" should {
          "Have one record" in {
            bufferTotalCount shouldBe 1
            val currentRecordsList = {
              payloadBufferRepository.list
            }
            val res = payloadBufferRepository.deleteOne(createPayloadWrapper(testPayloadDeletes))

            whenReady(res) { result =>
              result shouldBe ((): Unit)
              bufferTotalCount shouldBe 0
            }
        }

        "there are multiple records in the database" should {
          "Have three records" in {
            insertMany(List(getVatRegCompany(testVatNo1), getVatRegCompany(testVatNo2), getVatRegCompany(testVatNo3)))
            bufferTotalCount shouldBe 0
          }
      }
    }
=======
>>>>>>> 3363962 (Update test data and tests to improve clarity)
  }
}
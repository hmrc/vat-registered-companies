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

package uk.gov.hmrc.vatregisteredcompanies.schedulers

import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData._

class PayloadConversionSchedulerISpec extends IntegrationSpecBase {

  override def beforeEach(): Unit = {
    deleteAll
    clearLock()
    deleteAllBuffer
  }
  "processOneData" when {
    "the lock is not already acquired" should {
      "get the oldest payload from the buffer repository and process it then delete buffer record" when {
        "there is one record in the buffer and no records in vatRegisteredCompanies" that {
          "contains a payload with only createsAndUpdates" in {


            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates)
            Thread.sleep(100)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted into vatRegisteredCompanies database
              totalCount shouldBe 2
              val checkCompanyInserted = vatRegisteredCompaniesRepository.collection.find().toFuture()
                Thread.sleep(100)
              checkCompanyInserted.toString should include(testVatNo1)
              checkCompanyInserted.toString should include(testVatNo2)
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              isLocked shouldBe false
            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes)
            Thread.sleep(500)
            bufferTotalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              vatRegisteredCompaniesRepository.collection.find().toFuture().value shouldBe empty

              //check buffer record deleted
              //check lock has been removed
              bufferTotalCount shouldBe 0
              totalCount shouldBe 0
              isLocked shouldBe false
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            bufferTotalCount shouldBe 0
            insertOneBuffer(testPayloadCreateAndDeletes)
                //this inserts companies with vatNo1 and vatNo2
                //deletes is deleting vatNo1 and vatNo3
            Thread.sleep(100)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            val payloadSentToBufferRepo = await(payloadBufferRepository.list)
            payloadSentToBufferRepo.mkString should include (testVatNo1)
            payloadSentToBufferRepo.mkString should include (testVatNo2)

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
                val checkCompanyInserted = vatRegisteredCompaniesRepository.collection.find().toFuture()
                Thread.sleep(500)
                checkCompanyInserted.toString should include(testVatNo2)
              //check records deleted from vatRegisteredCompanies database
                checkCompanyInserted.toString shouldNot include(testVatNo1)

              totalCount shouldBe 1
              val secondCompanyStillPresent = vatRegisteredCompaniesRepository.collection.find().toFuture()
              Thread.sleep(500)
              secondCompanyStillPresent.toString shouldNot  include (testVatNo1)
              secondCompanyStillPresent.toString should include (testVatNo2)
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              isLocked shouldBe false

              //check buffer record deleted
              //check lock has been removed

            }
          }
        }

        "there is one record in the buffer" that {
          "contains a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates)
            Thread.sleep(100)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0
            Thread.sleep(20)
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer payload
            insertOne(deltaTradingWithVatNo1)
            totalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
                  val checkCompanyInserted = vatRegisteredCompaniesRepository.collection.find().toFuture()
                  Thread.sleep(500)
              checkCompanyInserted.futureValue.head.company shouldBe deltaTradingWithVatNo1
              checkCompanyInserted.futureValue.last.company shouldBe acmeTradingWithVatNo2
              totalCount shouldBe 3
                  // 2 records in buffer were inserted.  1 record through vat company
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              isLocked shouldBe false
            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes)
            Thread.sleep(100)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
            insertOne(deltaTradingWithVatNo1)
            Thread.sleep(100)
            totalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records deleted from vatRegisteredCompanies database
              vatRegisteredCompaniesRepository.collection.find().toFuture().value shouldBe empty
              Thread.sleep(100)
              totalCount shouldBe 0
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              isLocked shouldBe false
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            insertOneBuffer(testPayloadCreateAndDeletes1)
            //this inserts companies with vatNo1 and vatNo2
            //deletes is deleting vatNo2 and vatNo3
            Thread.sleep(100)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0
            Thread.sleep(20)
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
            insertOne(deltaTradingWithVatNo1)
            totalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              val checkCompanyInserted = vatRegisteredCompaniesRepository.collection.find().toFuture()
                  // 2 companies were inserted, 1 present through VRC insertion = 3, 2 listed for delete, only 1 found = 2 companies present.
              checkCompanyInserted.futureValue.head.company shouldBe deltaTradingWithVatNo1
              checkCompanyInserted.futureValue.last.company shouldBe acmeTradingWithVatNo1
              Thread.sleep(100)
              totalCount shouldBe 2

              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              isLocked shouldBe false
            }
          }
        }

        "there is multiple records in the buffer" that {
          "has the oldest record containing a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates1)
                //inserts 2 companies with vatNo3 and vatNo4
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            insertOneBuffer(testPayloadCreateAndDeletes1)
                //inserts 2 companies with vatNo1 and vatNo2 AND deletes records with vatNo 2 and 3
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes1)
            //deletes records with vatNo 1

            Thread.sleep(50)
            bufferTotalCount shouldBe 3

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              val checkCompanyInserted = vatRegisteredCompaniesRepository.collection.find().toFuture()
//              // 2 companies were inserted
              checkCompanyInserted.futureValue.head.company shouldBe acmeTradingWithVatNo3
              totalCount shouldBe 2

              //check buffer record with payload containing only createsandupdates is deleted
              val bufferList = await(payloadBufferRepository.list)
              Thread.sleep(50)
              bufferTotalCount shouldBe 2
              bufferList.head.payload.createsAndUpdates.mkString shouldNot include (testVatNo3)
              bufferList.head.payload shouldBe testPayloadCreateAndDeletes1

              //check lock has been removed
              isLocked shouldBe false
            }
          }

          "has a oldest record containing a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes1)
                //deletes records with vatNo 1
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates1)
                //inserts 2 companies with vatNo3 and vatNo4
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            insertOneBuffer(testPayloadCreateAndDeletes1)
                //inserts 2 companies with vatNo1 and vatNo2 AND deletes records with vatNo 2 and 3
            Thread.sleep(500)
            bufferTotalCount shouldBe 3
            // insert records in vatRegisteredCompanies to be deleted
            insertOne(acmeTradingWithVatNo4)
            insertOne(acmeTradingWithVatNo2)
            insertOne(acmeTradingWithVatNo1)
            insertOne(acmeTradingWithVatNo3)
            Thread.sleep(500)
            totalCount shouldBe 4

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              totalCount shouldBe 3
              val records = vatRegisteredCompaniesRepository.collection.find().toFuture()
              records.futureValue.apply(2).company shouldBe acmeTradingWithVatNo3
              records.futureValue.toString shouldNot include (testVatNo1)

              //check buffer record deleted with payload containing only deletes
              val bufferList = await(payloadBufferRepository.list)
              Thread.sleep(50)
              bufferTotalCount shouldBe 2
              bufferList.head.payload shouldBe testPayloadCreateAndUpdates1
              bufferList.tail.last.payload shouldBe testPayloadCreateAndDeletes1

              //check lock has been removed
              isLocked shouldBe false
            }
          }

          "has a oldest record containing a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            insertOneBuffer(testPayloadCreateAndDeletes1)
            //inserts 2 companies with vatNo1 and vatNo2 AND deletes records with vatNo 2 and 3
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes1)
            //deletes records with vatNo 1
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates1)
            //inserts 2 companies with vatNo3 and vatNo4
            Thread.sleep(100)
            bufferTotalCount shouldBe 3

            // insert records in vatRegisteredCompanies to be deleted
            insertOne(acmeTradingWithVatNo4)
            insertOne(acmeTradingWithVatNo2)
            insertOne(acmeTradingWithVatNo1)
            insertOne(acmeTradingWithVatNo3)
            Thread.sleep(100)
            totalCount shouldBe 4

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted or deleted into vatRegisteredCompanies database
              totalCount shouldBe 3
              //after buffer insert - there would be 6 companies (vat 1 x 2, vat 2 x 2, vat 3, vat 4
              // then the deletes would remove any vat2 and vat 3 companies (-3 records)

              val records = vatRegisteredCompaniesRepository.collection.find().toFuture()
              records.futureValue.apply(0).company shouldBe acmeTradingWithVatNo4
              records.futureValue.apply(1).company shouldBe acmeTradingWithVatNo1
              records.futureValue.apply(2).company shouldBe acmeTradingWithVatNo1
              records.futureValue.toString shouldNot include (testVatNo2)
              records.futureValue.toString shouldNot include(testVatNo3)
//              //check buffer record deleted with payload containing createsAndUpdates and deletes
              val bufferList = await(payloadBufferRepository.list)
              Thread.sleep(100)
              bufferTotalCount shouldBe 2
              bufferList.head.payload shouldBe testPayloadDeletes1
              bufferList.tail.last.payload shouldBe testPayloadCreateAndUpdates1

              //check lock has been removed
              isLocked shouldBe false
            }
          }
        }
      }
    }

    "the lock is already acquired" that {
      "is within the TTL" should {
        "not run the processing job and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          insertOneBuffer(testPayloadCreateAndDeletes1)
          //inserts 2 companies with vatNo1 and vatNo2 AND deletes records with vatNo 2 and 3
          Thread.sleep(100)
          bufferTotalCount shouldBe 1
          // insert records in vatRegisteredCompanies to be deleted
          insertOne(acmeTradingWithVatNo4)
          insertOne(acmeTradingWithVatNo2)
          insertOne(acmeTradingWithVatNo1)
          insertOne(acmeTradingWithVatNo3)
          Thread.sleep(100)
          totalCount shouldBe 4
          // insert a lock within TTL
          insert(testLock)
          isLocked shouldBe true

          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted or deleted into vatRegisteredCompanies database
            totalCount shouldBe 4

            val records = vatRegisteredCompaniesRepository.collection.find().toFuture()
            records.futureValue.apply(2).company shouldBe acmeTradingWithVatNo1
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            bufferTotalCount shouldBe 1

            val bufferList = await(payloadBufferRepository.list)
            bufferList.head.payload shouldBe testPayloadCreateAndDeletes1
            //check lock is still present
            isLocked shouldBe true
          }
        }
      }

      "is outside of the TTL" should {
        "not run the processing job, remove the lock and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          insertOneBuffer(testPayloadCreateAndDeletes1)
          Thread.sleep(500)
          //inserts 2 companies with vatNo1 and vatNo2 AND deletes records with vatNo 2 and 3
          bufferTotalCount shouldBe 1
          // insert records in vatRegisteredCompanies to be deleted
          insertOne(acmeTradingWithVatNo4)
          insertOne(acmeTradingWithVatNo2)
          insertOne(acmeTradingWithVatNo1)
          insertOne(acmeTradingWithVatNo3)
          Thread.sleep(100)
          totalCount shouldBe 4
          // insert a lock outside TTL
          insert(expiredTestLock)
          isLocked shouldBe true

          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted or deleted into vatRegisteredCompanies database
            totalCount shouldBe 4
            val records = vatRegisteredCompaniesRepository.collection.find().toFuture()
            records.futureValue.apply(2).company shouldBe acmeTradingWithVatNo1
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            bufferTotalCount shouldBe 1
            val bufferList = await(payloadBufferRepository.list)
            bufferList.head.payload shouldBe testPayloadCreateAndDeletes1
            //check lock has been removed
            isLocked shouldBe false
          }
        }
      }
    }
  }
}

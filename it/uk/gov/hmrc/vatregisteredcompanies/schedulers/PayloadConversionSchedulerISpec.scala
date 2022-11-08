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

  "processOneData" when {
    "the lock is not already acquired" should {
      "get the oldest payload from the buffer repository and process it then delete buffer record" when {
        "there is one record in the buffer and no records in vatRegisteredCompanies" that {
          "contains a payload with only createsAndUpdates" in {


            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted into vatRegisteredCompanies database
              totalCount shouldBe 2
              val checkCompanyInserted = await(vatRegisteredCompaniesRepository.findAll())
              checkCompanyInserted.mkString should include(testVatNo1)
              checkCompanyInserted.mkString should include(testVatNo2)
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0

              //check records inserted into vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed

            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes

            insertOneBuffer(testPayloadDeletes)
            bufferTotalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database

              //TODO: based on above request, no records would be present. - Will have another dev confirm assumption
              //check buffer record deleted
              //check lock has been removed
              bufferTotalCount shouldBe 0
              totalCount shouldBe 0
              lockCount shouldBe 0

              //check buffer record deleted
              //check lock has been removed

            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes

            bufferTotalCount shouldBe 0
            insertOneBuffer(testPayloadCreateAndDeletes)
                //this inserts companies with vatNo1 and vatNo2
                //deletes is deleting vatNo1 and vatNo3

            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            val payloadSentToBufferRepo = await(payloadBufferRepository.list)
            payloadSentToBufferRepo.mkString should include (testVatNo1)
            payloadSentToBufferRepo.mkString should include (testVatNo2)
            // payloadSentToBufferRepo.head.payload should include(testPayloadCreateAndUpdates.toString)


            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database

              totalCount shouldBe 1
              val secondCompanyStillPresent = await(vatRegisteredCompaniesRepository.findAll())
              secondCompanyStillPresent.mkString shouldNot  include (testVatNo1)
              secondCompanyStillPresent.mkString should include (testVatNo2)
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0

              //check buffer record deleted
              //check lock has been removed

            }
          }
        }

        "there is one record in the buffer" that {
          "contains a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates

            insertOneBuffer(testPayloadCreateAndUpdates)
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
              val checkCompanyInserted = await(vatRegisteredCompaniesRepository.findAll())
              checkCompanyInserted.head.company shouldBe deltaTradingWithVatNo1
              checkCompanyInserted.last.company shouldBe acmeTradingWithVatNo2
              totalCount shouldBe 3
                // 2 records in buffer were inserted.  1 record through vat company
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0

            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0
            Thread.sleep(20)
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
            insertOne(deltaTradingWithVatNo1)
            totalCount shouldBe 1

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records deleted from vatRegisteredCompanies database
              val checkCompanyInserted = await(vatRegisteredCompaniesRepository.findAll())
              checkCompanyInserted shouldBe empty
              totalCount shouldBe 0
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            insertOneBuffer(testPayloadCreateAndDeletes1)
              //this inserts companies with vatNo1 and vatNo2
              //deletes is deleting vatNo2 and vatNo3
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
              val checkCompanyInserted = await(vatRegisteredCompaniesRepository.findAll())
                // 2 companies were inserted, 1 present through VRC insertion = 3, 2 listed for delete, only 1 found = 2 companies present.
              checkCompanyInserted.head.company shouldBe deltaTradingWithVatNo1
              checkCompanyInserted.last.company shouldBe acmeTradingWithVatNo1
              totalCount shouldBe 2

              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0
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

              val checkCompanyInserted = await(vatRegisteredCompaniesRepository.findAll())
              // 2 companies were inserted
              checkCompanyInserted.head.company shouldBe acmeTradingWithVatNo3
              totalCount shouldBe 2

              //check buffer record with payload containing only createsandupdates is deleted
              val bufferList = await(payloadBufferRepository.list)

              bufferTotalCount shouldBe 2
              bufferList.head.payload.createsAndUpdates.mkString shouldNot include (testVatNo3)
              bufferList.head.payload shouldBe testPayloadCreateAndDeletes1

              //check lock has been removed
              lockCount shouldBe 0
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
            bufferTotalCount shouldBe 3
            // insert records in vatRegisteredCompanies to be deleted
            insertOne(acmeTradingWithVatNo4)
            insertOne(acmeTradingWithVatNo2)
            insertOne(acmeTradingWithVatNo1)
            insertOne(acmeTradingWithVatNo3)
            totalCount shouldBe 4

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              totalCount shouldBe 3
              val records = await(vatRegisteredCompaniesRepository.findAll())
              records(2).company shouldBe acmeTradingWithVatNo3
              records.mkString shouldNot include (testVatNo1)

              //check buffer record deleted with payload containing only deletes
              val bufferList = await(payloadBufferRepository.list)

              bufferTotalCount shouldBe 2
              bufferList.head.payload shouldBe testPayloadCreateAndUpdates1
              bufferList.tail.last.payload shouldBe testPayloadCreateAndDeletes1

              //check lock has been removed
              lockCount shouldBe 0
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
            bufferTotalCount shouldBe 3

            // insert records in vatRegisteredCompanies to be deleted
            insertOne(acmeTradingWithVatNo4)
            insertOne(acmeTradingWithVatNo2)
            insertOne(acmeTradingWithVatNo1)
            insertOne(acmeTradingWithVatNo3)
            totalCount shouldBe 4

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              totalCount shouldBe 3
                  //after buffer insert - there would be 6 companies (vat 1 x 2, vat 2 x 2, vat 3, vat 4
                  // then the deletes would remove any vat2 and vat 3 companies (-3 records)
              val records = await(vatRegisteredCompaniesRepository.findAll())
                  records(0).company shouldBe acmeTradingWithVatNo4
                  records(1).company shouldBe acmeTradingWithVatNo1
                  records(2).company shouldBe acmeTradingWithVatNo1
                  records.mkString shouldNot include(testVatNo2)
                  records.mkString shouldNot include(testVatNo3)
              //check buffer record deleted with payload containing createsAndUpdates and deletes
              val bufferList = await(payloadBufferRepository.list)

              bufferTotalCount shouldBe 2
              bufferList.head.payload shouldBe testPayloadDeletes1
              bufferList.tail.last.payload shouldBe testPayloadCreateAndUpdates1

              //check lock has been removed
              lockCount shouldBe 0
            }
          }
        }
      }
    }

    "the lock is already acquired" that {
      "is within the TTL" should {
        "not run the processing job and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          //insert records in vatRegisteredCompanies to be deleted
          // insert a lock within TTL
          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted into vatRegisteredCompanies database
            //check no records deleted from vatRegisteredCompanies database
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            //check lock is still present removed
          }

        }
      }

      "is outside of the TTL" should {
        "not run the processing job, remove the lock and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          //insert records in vatRegisteredCompanies to be deleted
          // insert a lock within TTL
          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted into vatRegisteredCompanies database
            //check no records deleted from vatRegisteredCompanies database
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            //check lock has been removed
          }
        }
      }
    }
  }
}

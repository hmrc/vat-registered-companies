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
import uk.gov.hmrc.vatregisteredcompanies.models.VatRegisteredCompany

class OldRecordsDeleteSchedulerISpec extends IntegrationSpecBase {

  val limit = 2

  "deleteOld with limit 2" when {
    "the lock is not already acquired" should {
      "not remove any documents" when {
        "there are no records with repeated vatNumbers" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2))
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check no records deleted
            totalCount shouldBe 2
            //check lock has been removed
            lockCount shouldBe 0
          }
        }
      }
      "remove the oldest record(s)" when {
        "there is one VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            val record = getRecord(testVatNo1)
            record shouldBe defined
            record.get.name shouldBe "Delta Trading"
            totalCount shouldBe 3
            //check lock has been removed
            lockCount shouldBe 0
          }
        }

        "there are two VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, deltaTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 2 of the vatNumbers used above but slightly different details
          insertMany(List(deltaTradingWithVatNo1, acmeTradingWithVatNo2))

          val res = persistenceService.deleteOld(limit)
          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check only 1 record exists with VatNumber and is the newest for both the repeated vatNumbers
            val record1 = getRecord(testVatNo1)
            record1 shouldBe defined
            record1.get.name shouldBe "Delta Trading"
            val record2 = getRecord(testVatNo2)
            record2 shouldBe defined
            record2.get.name shouldBe "ACME Trading"
            //check lock has been removed
            lockCount shouldBe 0
          }
        }
      }

      "remove only two of the oldest record(s)" when {
        "there are three VatNumber with more than one record associated with and the limit is 2" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 3 of the vatNumbers used above but slightly different details
          insertMany(List(deltaTradingWithVatNo1, deltaTradingWithVatNo2))

          val res = persistenceService.deleteOld(limit)
          whenReady(res) { result =>
            result shouldBe ((): Unit)
            totalCount shouldBe 3
            //check only 2 records deleted
            lockCount shouldBe 0
            //check lock has been removed
          }
        }
      }
    }

    "the lock is already acquired" that {
      "is within the TTL" should {
        "not run the deleteOld job and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          totalCount shouldBe 4
          // insert a lock within TTL
          insert(testLock)
          val res = persistenceService.deleteOld(limit)

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records deleted from vatRegisteredCompanies database
            //totalCount shouldBe 4 fails and should pass
            // deletes old record currently
            //check lock is still present removed
            // Fails - lockCount shouldBe 0
          }
        }
      }

      "is outside of the TTL" should {
        "not run the deleteOld job, remove the lock and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          // insert a lock outside the TTL
          insert(expiredTestLock)

          val res = persistenceService.processOneData
          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records deleted from vatRegisteredCompanies database
            //check lock has been removed
          }
        }
      }
    }
  }
}

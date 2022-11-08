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

class OldRecordsDeleteSchedulerISpec extends IntegrationSpecBase {

  val limit = 2

  "deleteOld with limit 2" when {
    "the lock is not already acquired" should {
      "not remove any documents" when {
        "there are no records with repeated vatNumbers" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check no records deleted
            //check lock has been removed
          }
        }
      }
      "remove the oldest record(s)" when {
        "there is one VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          //Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check only 1 record exists with VatNumber and is the newest
            //check lock has been removed
          }
        }

        "there are two VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          //Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 2 of the vatNumbers used above but slightly different details
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check only 1 record exists with VatNumber and is the newest for both the repeated vatNumbers
            //check lock has been removed
          }
        }
      }

      "remove only two of the oldest record(s)" when {
        "there are three VatNumber with more than one record associated with and the limit is 2" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          //Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 3 of the vatNumbers used above but slightly different details
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check only 2 records deleted
            //check lock has been removed
          }
        }
      }
    }

    "the lock is already acquired" that {
      "is within the TTL" should {
        "not run the deleteOld job and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          //Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          // insert a lock within TTL
          val res = persistenceService.deleteOld(limit)

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records deleted from vatRegisteredCompanies database
            //check lock is still present removed
          }

        }
      }

      "is outside of the TTL" should {
        "not run the deleteOld job, remove the lock and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          //Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          // insert a lock within TTL
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

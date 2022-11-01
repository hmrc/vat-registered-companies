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

class VatRegisteredCompaniesRepositoryISpec extends IntegrationSpecBase {
  override def beforeEach(): Unit = {
    await(vatRegisteredCompaniesRepository.deleteAll())
  }

  "Method: deleteAll" when {
    "there are no records in the database" should {
      "Have no records" in {
        totalCount shouldBe 0
      }
    }

    "there is 1 record in the database" should {
      "Have one record" in {
        insertOne(getVatRegCompany(testVatNo1))
        totalCount shouldBe 1
      }
    }

    "there are multiple records in the database" should {
      "Have three records" in {
        insertMany(List(getVatRegCompany(testVatNo1), getVatRegCompany(testVatNo2), getVatRegCompany(testVatNo3)))
        totalCount shouldBe 3
      }
    }
  }

  "Method: process" when {
    "the payload contains only create and updates" should {
      "return unit and insert into database" when {

        "the database has no records" in {
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 2
          }
        }

        "the database has one record" in {
          insertOne(getVatRegCompany(testVatNo3))
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 3
          }
        }

        "the database has multiple records, including duplicate VAT number" in {
          insertOne(getVatRegCompany(testVatNo1))
          insertOne(getVatRegCompany(testVatNo3))
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 4
          }
        }

      }
    }
  }

  "Method: lookup" should {
    "return the latest lookup response with the vatRegisteredCompany" when {
      "the database has no records" in {
        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)
        whenReady(result) { res =>
          res shouldBe None
        }
      }

      "the database has a record with the matching vatNumber" in {
        insertOne(getVatRegCompany(testVatNo1))
        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)

        whenReady(result) { res =>
          res shouldBe defined
          res.get.target shouldBe defined
          res.get.target.get.vatNumber shouldBe testVatNo1
        }
      }

      "the database has more than one record with the vatNumber" in {
        val oldestRecord = getVatRegCompany(testVatNo1)
        val newestRecord = oldestRecord.copy(name = "newCompany")
        insertOne(oldestRecord)
        Thread.sleep(100)
        insertOne(newestRecord)

        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)
          whenReady(result) {res =>
            res shouldBe defined
            res.get.target shouldBe defined
            res.get.target.get.name shouldBe "newCompany"
          }
      }
    }
  }
}
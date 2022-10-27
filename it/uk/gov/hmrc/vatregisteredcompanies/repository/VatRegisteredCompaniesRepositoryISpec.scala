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

class VatRegisteredCompaniesRepositoryISpec extends IntegrationSpecBase {
  private def clearDB(): Unit = {
    val result = vatRegisteredCompaniesRepository.deleteAll()

    whenReady(result) { res =>
      res shouldBe ((): Unit)
      totalCount shouldBe 0
    }
  }

  "deleteAll" when {
    "there are no records in the database" should {
      "return unit" in {
        clearDB()
      }
    }

    "there is 1 record in the database" should {
      "return unit" in {
        insertOne(getVatRegCompany(testVatNo1))
        totalCount shouldBe 1

        clearDB()
      }
    }

    "there are multiple records in the database" should {
      "return unit" in {
        insertOne(getVatRegCompany(testVatNo1))
        insertOne(getVatRegCompany(testVatNo2))
        insertOne(getVatRegCompany(testVatNo3))
        totalCount shouldBe 3

        clearDB()
      }
    }
  }

  "process" when {

    "the payload contains only create and updates" should {
      "return unit and insert into database" when {
        "the database has no records" in {
          // assemble
          // no records
          // act
          vatRegisteredCompaniesRepository.process(testPayload1)
          // assertion
          totalCount shouldBe 0

          clearDB()
        }
//        "the database has one record" in {
//
//        }
//
//        "the database has multiple records, including duplicate VAT number" in {
//
//        }
      }
    }

//    "the payload contains only deletes" should {
//      "return unit and deletes the records if present in the database" when {
//        "the database has no records" in {
//          val result = vatRegisteredCompaniesRepository.process(testPayload)
//          totalCount
//        }
//        "the database has one record that matches the VAT number" in {
//
//        }
//
//        "the database has multiple records that matches the VAT number" in {
//
//        }
//
//        "the database has one record that doesn't match the VAT number" in {
//
//        }
//
//        "the database has multiple records that doesn't match the VAT number" in {
//
//        }
//
//        "the database has multiple records, including duplicate VAT number" in {
//
//        }
      }
}

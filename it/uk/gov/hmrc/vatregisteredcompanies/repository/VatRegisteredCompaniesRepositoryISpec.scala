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

  "deleteAll" when {
    "there are no records in the database" should {
      "return unit" in {
        val result = vatRegisteredCompaniesRepository.deleteAll()

        whenReady(result)  { res =>
          res shouldBe ((): Unit)
          totalCount shouldBe 0
        }
      }
    }

    "there is 1 record in the database" should {
      "return unit" in {
        insertOne(getVatRegCompany(testVatNo1))
        totalCount shouldBe 1
        val result = vatRegisteredCompaniesRepository.deleteAll()

        whenReady(result)  { res =>
          res shouldBe ((): Unit)
          totalCount shouldBe 0
        }
      }
    }
  }

}

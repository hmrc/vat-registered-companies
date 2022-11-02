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

class LockRepositoryISpec extends IntegrationSpecBase {

  "Method: release" when {
    "no lock exists" should {
      "Return this" in {
        val result = lockRepository.release(testLockId)

        whenReady(result) { res =>
          res shouldBe ((): Unit)
        }
      }
    }

    "A lock exists" should {
      "Return this" in {
        lockRepository.lock(2)
        val result = lockRepository.release(testLockId)

        whenReady(result) { res =>
          res shouldBe ((): Unit)
        }
      }
    }
  }
}

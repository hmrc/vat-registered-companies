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
import uk.gov.hmrc.vatregisteredcompanies.repositories.Lock

class LockRepositoryISpec extends IntegrationSpecBase {

  "Method: release" when {
    "no lock exists" should {
      "Return (): Unit" in {
        val result = lockRepository.release(testLockId)

        whenReady(result) { res =>
          res shouldBe ((): Unit)
        }
      }
    }

    "A lock exists" should {
      "Return (): Unit" in {
        lockRepository.lock(2)
        val result = lockRepository.release(testLockId)

        whenReady(result) { res =>
          res shouldBe ((): Unit)
        }
      }
    }
  }

  "Method: isLocked" when {
    "no lock exists" should {
      "Return false" in {
        val result = lockRepository.isLocked(testLockId)

        whenReady(result) { res =>
          res shouldBe false
        }
      }
    }

    "A lock exists" should {
      "Return true" in {
        lockRepository.lock(testLockId)
        val result = lockRepository.isLocked(testLockId)

        whenReady(result) { res =>
          res shouldBe true
        }
      }
    }
  }

  "Method: getLock" when {
    "no lock exists" should {
      "Return None" in {
        val result = lockRepository.getLock(testLockId)

        whenReady(result) { res =>
          res shouldBe None
        }
      }
    }

    "A lock exists" should {
      "Return true" in {
        lockRepository.lock(testLockId)
        val result = lockRepository.getLock(testLockId)

        whenReady(result) { res =>
          res shouldBe defined
          res.get._id shouldBe testLockId
        }
      }
    }
  }
}

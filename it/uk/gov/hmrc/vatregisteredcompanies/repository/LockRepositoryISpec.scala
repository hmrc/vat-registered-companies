/*
 * Copyright 2024 HM Revenue & Customs
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
   override def beforeEach(): Unit = {
     clearLock()
  }

  "Method: lock" when {
    "no lock exists" should {
      "Return true" in {
        val res = lockRepository.lock(testLockId)

        whenReady(res) { res =>
          res shouldBe true
        }
      }
    }

    "lock exists within TTL" should {
      "Return false" in {
        insert(testLock)
        val act = lockRepository.lock(testLockId)


        whenReady(act) { res =>
          res shouldBe false
          isLocked shouldBe true
        }
      }
    }
    "lock exists outside TTL" should {
      "Return false" in {
        insert(expiredTestLock)

        val act = lockRepository.lock(testLockId)

        whenReady(act) { res =>
          res shouldBe false
          Thread.sleep(5000)
          isLocked shouldBe false
        }
      }
    }

  }

  "Method: release" when {
    "no lock exists" should {
      "Return (): Unit" in {
        val act = lockRepository.release(testLockId)

        whenReady(act) { res =>
          res shouldBe ((): Unit)
        }
      }
    }

    "A lock exists" should {
      "Return (): Unit" in {
        insert(testLock)
        val act = lockRepository.release(testLockId)

        whenReady(act) { res =>
          res shouldBe ((): Unit)
        }
      }
    }
  }

  "Method: isLocked" when {
    "no lock exists" should {
      "Return false" in {
        val act = lockRepository.isLocked(testLockId)

        whenReady(act) { res =>
          res shouldBe false
        }
      }
    }

    "A lock exists" should {
      "Return true" in {
        insert(testLock)
        Thread.sleep(5000)
        val act = lockRepository.isLocked(testLockId)

        whenReady(act) { res =>
          res shouldBe true
        }
      }
    }
  }

  "Method: getLock" when {
    "no lock exists" should {
      "Return None" in {
        val act = lockRepository.getLock(testLockId)

        whenReady(act) { res =>
          res shouldBe None
        }
      }
    }

    "A lock exists" should {
      "Return true" in {
        insert(testLock)
        val act = lockRepository.getLock(testLockId)

        whenReady(act) { res =>
          res shouldBe defined
          res.get._id shouldBe testLockId
        }
      }
    }
  }
}

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

package uk.gov.hmrc.vatregisteredcompanies.helpers
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.vatregisteredcompanies.repositories.{Lock, LockRepository}
import uk.gov.hmrc.vatregisteredcompanies.repositories.Lock._

trait LockDatabaseOperations {

  self: IntegrationSpecBase =>

  val lockRepository: LockRepository
  val testLockId = 1

  def insert(lock: Lock): Unit = {
    await(lockRepository.collection.insertOne(lock).toFuture())
  }

  def clearLock(): Unit = {
    await(lockRepository.collection.deleteOne(Filters.empty()).toFuture())
  }

  def isLocked: Boolean = {
    await(lockRepository.isLocked(testLockId))
  }
}

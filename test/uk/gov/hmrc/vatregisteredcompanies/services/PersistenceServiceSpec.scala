/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.vatregisteredcompanies.services

import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.vatregisteredcompanies.models.Payload
import uk.gov.hmrc.vatregisteredcompanies.repositories.{LockRepository, PayloadBufferRepository, PayloadWrapper, VatRegisteredCompaniesRepository}

import scala.concurrent.{ExecutionContext, Future, Promise}

class PersistenceServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  "processOneData" should {
    "wait for index initialization before selecting a payload or acquiring the processing lock" in {
      val repository = mock[VatRegisteredCompaniesRepository]
      val buffer = mock[PayloadBufferRepository]
      val locks = mock[LockRepository]
      val initialized = Promise[Unit]()
      val payload = PayloadWrapper(payload = Payload(Nil, Nil))
      when(repository.initialised).thenReturn(initialized.future)
      when(buffer.one).thenReturn(Future.successful(Some(payload)))
      when(locks.lock(1)).thenReturn(Future.successful(true))
      when(locks.release(1)).thenReturn(Future.unit)
      when(repository.process(payload)).thenReturn(Future.unit)
      val service = new PersistenceService(repository, buffer, locks)

      val result = service.processOneData

      result.isCompleted shouldBe false
      verifyNoInteractions(buffer, locks)

      initialized.success(())
      result.futureValue shouldBe ((): Unit)
      verify(buffer).one
      verify(locks).lock(1)
      verify(repository).process(payload)
      verify(locks).release(1)
    }

    "propagate index initialization failure without selecting or processing a payload" in {
      val repository = mock[VatRegisteredCompaniesRepository]
      val buffer = mock[PayloadBufferRepository]
      val locks = mock[LockRepository]
      val failure = new RuntimeException("Index creation failed")
      when(repository.initialised).thenReturn(Future.failed(failure))
      val service = new PersistenceService(repository, buffer, locks)

      service.processOneData.failed.futureValue shouldBe failure

      verifyNoInteractions(buffer, locks)
      verify(repository, never()).process(org.mockito.ArgumentMatchers.any[PayloadWrapper]())
    }
  }
}

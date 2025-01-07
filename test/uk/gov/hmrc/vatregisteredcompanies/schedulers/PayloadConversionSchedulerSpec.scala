/*
 * Copyright 2025 HM Revenue & Customs
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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService
import uk.gov.hmrc.vatregisteredcompanies.schedulers.PayloadConversionScheduler
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PayloadConversionSchedulerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "PayloadConversionScheduler" should {


    "not schedule processing of data when disabled" in {
      // Mock dependencies
      val persistenceServiceMock = mock[PersistenceService]
      val actorSystemMock = mock[ActorSystem]
      val schedulerMock = mock[Scheduler]
      val configMock = mock[play.api.Configuration]

      // Mocking configuration values
      when(configMock.getOptional[Int]("microservice.services.schedulers.payload.conversion.interval.seconds")).thenReturn(Some(1))
      when(configMock.getOptional[Boolean]("microservice.services.schedulers.payload.conversion.enabled")).thenReturn(Some(false))

      // Mock scheduler
      when(actorSystemMock.scheduler).thenReturn(schedulerMock)

      // Create an ExecutionContext for the scheduler
      implicit val ec: ExecutionContext = ExecutionContext.global

      // Create the PayloadConversionScheduler instance with mocks
      val scheduler = new PayloadConversionScheduler(
        persistenceServiceMock,
        actorSystemMock,
        FiniteDuration(1, SECONDS), // Test interval is 1 second for quicker testing
        enabled = false // Disabled for this test
      )(ec)

      // Verify that the processOneData method was never called on PersistenceService
      verify(persistenceServiceMock, times(0)).processOneData
    }
 }
}

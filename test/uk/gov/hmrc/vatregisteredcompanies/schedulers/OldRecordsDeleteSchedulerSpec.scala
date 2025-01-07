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
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService
import uk.gov.hmrc.vatregisteredcompanies.schedulers.OldRecordsDeleteScheduler
import org.scalatest.matchers.should.Matchers

class OldRecordsDeleteSchedulerSpec extends AnyWordSpec with Matchers with MockitoSugar {



  "not schedule deletion of old records when disabled" in {
    // Mock dependencies
    val persistenceServiceMock = mock[PersistenceService]
    val actorSystemMock = mock[ActorSystem]
    val schedulerMock = mock[Scheduler]
    val configMock = mock[play.api.Configuration]

    // Mocking configuration values
    when(configMock.getOptional[Int]("microservice.services.schedulers.old-data-deletion.interval.seconds")).thenReturn(Some(1))
    when(configMock.getOptional[Boolean]("microservice.services.schedulers.old-data-deletion.enabled")).thenReturn(Some(false))
    when(configMock.getOptional[Int]("microservice.services.schedulers.old-data-deletion.rowCount")).thenReturn(Some(100))

    // Mock scheduler
    when(actorSystemMock.scheduler).thenReturn(schedulerMock)

    // Create an ExecutionContext for the scheduler
    implicit val ec: ExecutionContext = ExecutionContext.global

    // Create the OldRecordsDeleteScheduler instance with mocks
    val scheduler = new OldRecordsDeleteScheduler(
      persistenceServiceMock,
      actorSystemMock,
      FiniteDuration(1, SECONDS), // Test interval is 1 second for quicker testing
      enabled = false, // Disabled for this test
      rowCount = 100
    )(ec)

    // Verify that the deleteOld method was never called on PersistenceService
    verify(persistenceServiceMock, times(0)).deleteOld(100)
  }
//}
}

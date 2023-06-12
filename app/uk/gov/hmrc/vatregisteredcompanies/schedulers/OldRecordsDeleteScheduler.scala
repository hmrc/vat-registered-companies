/*
 * Copyright 2023 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import javax.inject.{Inject, Named, Singleton}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class OldRecordsDeleteScheduler @Inject()(
  persistenceService: PersistenceService,
  actorSystem: ActorSystem,
  @Named("interval") interval: FiniteDuration,
  @Named("enabled") enabled: Boolean,
  @Named("rowCount") rowCount: Int)(
  implicit val ec: ExecutionContext) {

  val logger = Logger(getClass)

  if (enabled) {
    logger.info(s"Initialising delete every $interval")
    actorSystem.scheduler.scheduleWithFixedDelay(FiniteDuration(60, TimeUnit.SECONDS), interval) { () =>
      logger.info(s"Scheduling old data delete, next run in $interval")
      persistenceService.deleteOld(rowCount).recover {
        case e: RuntimeException => logger.error(s"Error deleting old vat registration data: $e")
      }
    }
  }
}

class OldRecordsDeleteSchedulerModule(environment: Environment, val runModeConfiguration: Configuration) extends
  AbstractModule {

  @Provides
  @Named("interval")
  def interval(): FiniteDuration =
    new FiniteDuration(
      runModeConfiguration
        .getOptional[Int]("microservice.services.schedulers.old-data-deletion.interval.seconds")
        .getOrElse(600)
        .toLong,
      TimeUnit.SECONDS
    )

  @Provides
  @Named("enabled")
  def enabled(): Boolean =
    runModeConfiguration
      .getOptional[Boolean]("microservice.services.schedulers.old-data-deletion.enabled")
      .getOrElse(true)

  @Provides
  @Named("rowCount")
  def size(): Int =
    runModeConfiguration.getOptional[Int]("microservice.services.schedulers.old-data-deletion.rowCount")
      .getOrElse(100)

  override def configure(): Unit = {
    bind(classOf[OldRecordsDeleteScheduler]).asEagerSingleton()
  }

}

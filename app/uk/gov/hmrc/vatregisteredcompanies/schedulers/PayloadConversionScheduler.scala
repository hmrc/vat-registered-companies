/*
 * Copyright 2021 HM Revenue & Customs
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
class PayloadConversionScheduler @Inject()(
  persistenceService: PersistenceService,
  actorSystem: ActorSystem,
  @Named("payloadInterval") interval: FiniteDuration,
  @Named("payloadProcessingEnabled") enabled: Boolean)(
  implicit val ec: ExecutionContext) {

  val logger = Logger(getClass)

  if(enabled) {
    logger.info(s"Initialising payload processing every $interval")
    actorSystem.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS), interval) {
      logger.info(s"Scheduling inbound data processing, next run in $interval")
      persistenceService.processOneData.recover {
        case e: RuntimeException => logger.error(s"Error processing inbound vat registration data: $e")
      }
    }
  }

}

class PayloadConversionSchedulerModule(environment: Environment, val runModeConfiguration: Configuration) extends
  AbstractModule {

  @Provides
  @Named("payloadInterval")
  def interval(): FiniteDuration =
    new FiniteDuration(
      runModeConfiguration
        .getOptional[Int]("microservice.services.schedulers.payload.conversion.interval.seconds")
        .getOrElse(900)
        .toLong,
      TimeUnit.SECONDS
    )

  @Provides
  @Named("payloadProcessingEnabled")
  def enabled(): Boolean =
    runModeConfiguration
      .getOptional[Boolean]("microservice.services.schedulers.payload.conversion.enabled")
      .getOrElse(true)


  @Provides
  @Named("deletionThrottleElements")
  def throttleElements(): Int =
    runModeConfiguration
      .getOptional[Int]("microservice.services.schedulers.payload.conversion.deletion.throttle.elements")
      .getOrElse(500)

  @Provides
  @Named("deletionThrottlePer")
  def throttlePer(): FiniteDuration =
    new FiniteDuration(
      runModeConfiguration
        .getOptional[Int]("microservice.services.schedulers.payload.conversion.deletion.throttle.elements")
        .getOrElse(1)
        .toLong,
      TimeUnit.SECONDS
    )


  override def configure(): Unit = {
    bind(classOf[PayloadConversionScheduler]).asEagerSingleton()
  }

}

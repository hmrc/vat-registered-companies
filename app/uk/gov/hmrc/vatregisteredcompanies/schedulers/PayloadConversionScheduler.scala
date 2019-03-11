/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.{ActorSystem, Cancellable}
import com.google.inject.{AbstractModule, Provides}
import javax.inject.{Inject, Named, Singleton}

import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.{Duration, FiniteDuration}

@Singleton
class PayloadConversionScheduler @Inject()(
  persistenceService: PersistenceService,
  actorSystem: ActorSystem,
  @Named("interval") interval: FiniteDuration)(
  implicit val ec: ExecutionContext) {

  private val logger = Logger(getClass)

  logger.info(s"Initialising update every $interval")

  // TODO port to Actor to avoid job stopping on failure or overlapping
  actorSystem.scheduler.schedule(FiniteDuration(10, TimeUnit.SECONDS), interval) {
    logger.info("Scheduling inbound data processing")
    persistenceService.processData.recover {
      case e: RuntimeException => Logger.error(s"Error processing vat registration data: $e")
    }
  }

}

class PayloadConversionSchedulerModule(environment: Environment, val runModeConfiguration: Configuration) extends
  AbstractModule with ServicesConfig {
  override protected def mode: Mode = environment.mode

  @Provides
  @Named("interval")
  def interval(): FiniteDuration =
    new FiniteDuration(getConfInt("schedulers.payload.conversion.interval.seconds", 600).toLong, TimeUnit.SECONDS)

  override def configure(): Unit = {
    bind(classOf[PayloadConversionScheduler]).asEagerSingleton()
  }

}

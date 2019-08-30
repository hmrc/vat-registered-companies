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

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import javax.inject.{Inject, Named, Singleton}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class DeleteAllDataScheduler @Inject()(
  persistenceService: PersistenceService,
  actorSystem: ActorSystem,
  @Named("deleteAllDataEnabled") deleteAllDataEnabled: Boolean)(
  implicit val ec: ExecutionContext) {

  if (deleteAllDataEnabled) {
    actorSystem.scheduler.scheduleOnce(FiniteDuration(60, TimeUnit.SECONDS)) {
      Logger.info(s"Deleting all data!!!")
      persistenceService.deleteAll.recover {
        case e: RuntimeException => Logger.error(s"Error deleting all data: $e")
      }
    }
  }
}

class DeleteAllDataScheduleModule(environment: Environment, val runModeConfiguration: Configuration) extends
  AbstractModule {

  @Provides
  @Named("deleteAllDataEnabled")
  def deleteAllDataEnabled(): Boolean =
    runModeConfiguration
      .getBoolean("microservice.services.schedulers.all-data-deletion.enabled")
      .getOrElse(false)

  override def configure(): Unit = {
    bind(classOf[DeleteAllDataScheduler]).asEagerSingleton()
  }

}

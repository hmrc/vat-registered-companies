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

package uk.gov.hmrc.vatregisteredcompanies.helpers

import akka.http.scaladsl.model.HttpResponse
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, GivenWhenThen, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.vatregisteredcompanies.helpers.WiremockHelper.stubPost
import uk.gov.hmrc.vatregisteredcompanies.repositories.{DefaultLockRepository, LockRepository, PayloadBufferRepository, VatRegisteredCompaniesRepository}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait IntegrationSpecBase
    extends AnyWordSpec
    with GivenWhenThen
    with TestSuite
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with WiremockHelper
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Eventually
    with FutureAwaits
    with DefaultAwaitTimeout
    with PayloadBufferDatabaseOperations
    with VatRegisteredCompaniesDatabaseOperations
    with LockDatabaseOperations {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: Int    = WiremockHelper.wiremockPort
  val mockUrl          = s"http://$mockHost:$mockPort"

  val timeout: Timeout   = Timeout(Span(5, Seconds))
  val interval: Interval = Interval(Span(100, Millis))

  val DEFAULT_JOB_ENABLED       = "false"

  def config: Map[String, String] = Map(
    "application.router"                                      -> "testOnlyDoNotUseInAppConf.Routes",
    "auditing.consumer.baseUri.host"                          -> s"$mockHost",
    "auditing.consumer.baseUri.port"                          -> s"$mockPort",
    "microservice.services.auth.host"                         -> s"$mockHost",
    "microservice.services.auth.port"                         -> s"$mockPort",
    "microservice.services.schedulers.old-data-deletion.enabled" -> DEFAULT_JOB_ENABLED,
    "microservice.services.schedulers.all-data-deletion.enabled" -> DEFAULT_JOB_ENABLED,
    "microservice.services.schedulers.payload.conversion.enabled" -> DEFAULT_JOB_ENABLED
  )

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout  = scaled(Span(20, Seconds)),
    interval = scaled(Span(200, Millis))
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def statusOf(res: Future[HttpResponse])(implicit timeout: Duration): Int =
    Await.result(res, timeout).status.intValue()

  val lockRepository = app.injector.instanceOf[DefaultLockRepository]
  val payloadBufferRepository = app.injector.instanceOf[PayloadBufferRepository]
  val vatRegisteredCompaniesRepository = app.injector.instanceOf[VatRegisteredCompaniesRepository]

  val persistenceService = app.injector.instanceOf[PersistenceService]

  override def beforeEach(): Unit = {
    resetWiremock()
    await(lockRepository.drop)
    await(payloadBufferRepository.drop)
    await(vatRegisteredCompaniesRepository.drop)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(lockRepository.drop)
    await(payloadBufferRepository.drop)
    await(vatRegisteredCompaniesRepository.drop)
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  protected def stubAudit: StubMapping = stubPost(s"/write/audit", Status.OK)
}

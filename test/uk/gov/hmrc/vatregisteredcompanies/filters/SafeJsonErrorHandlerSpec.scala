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

package uk.gov.hmrc.vatregisteredcompanies.filters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HttpErrorHandler
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedEntry
import play.api.routing.{HandlerDef, Router}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.vatregisteredcompanies.testsupport.{Leakprobes, LogCapture}

import scala.concurrent.Await
import scala.concurrent.duration.*

class SafeJsonErrorHandlerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with LogCapture {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder().configure(
    Map(
    "auditing.enabled" -> "false",
    "microservice.services.schedulers.payload.conversion.enabled" -> false,
    "microservice.services.schedulers.old-data-deletion.enabled" -> false
    )
    ).build()
  }

    private lazy val handler = app.injector.instanceOf[HttpErrorHandler]

    private val controllerName = "uk.gov.hmrc.vatregisteredcompanies.controllers.VatRegCoLookupController"

    private val fourXxxExceptions: Seq[(String, Throwable, Int)] = Seq(
      ("a BadRequestException", new BadRequestException("bad request"), 400),
      ("an UpstreamErrorResponse with 404",  UpstreamErrorResponse("not found upstream", 404), 404)
    )

    "SafeJsonErrorHandler" should {

      "be the error handler the application uses" in {
        handler shouldBe a[SafeJsonErrorHandler]
      }

      Leakprobes.all.foreach {
        probe =>
          s"not log ${probe.label} when a request fails with a server error" in {
            val request = FakeRequest("GET", s"/vat-registered-companies/lookup/${probe.value}/${probe.value}")

            val logs = capturedLogs {
              val result = Await.result(handler.onServerError(request, new RuntimeException("500 server error")), 5.seconds)
              result.header.status shouldBe 500

            }
            logs should not be empty
            Leakprobes.leakingLines(logs, probe) shouldBe empty
          }

          fourXxxExceptions.foreach { case (name, exception, expectedStatus) =>

            s"not log ${probe.label} when $name is thrown" in {
              val request = FakeRequest("GET", s"/vat-registered-companies/lookup/${probe.value}/${probe.value}")

              val logs = capturedLogs {
                val result = Await.result(handler.onServerError(request, exception), 5.seconds)
                result.header.status shouldBe expectedStatus
              }

              logs should not be empty
              Leakprobes.leakingLines(logs, probe) shouldBe empty
            }
          }


          s"log the operation name instead of the path for ${probe.label}, in a routed request" in {
            val path = s"/vat-registered-companies/lookup/${probe.value}/${probe.value}"
            val request = FakeRequest("GET", path).addAttrs(
              TypedEntry(
                Router.Attrs.HandlerDef,
                HandlerDef(getClass.getClassLoader, "router", controllerName, "lookupVerified", Seq.empty, "GET", path, "", Seq.empty)

              )
            )

            val logs = capturedLogs {
              Await.result(handler.onServerError(request, new RuntimeException("")), 5.seconds)
            }
            logs.mkString("\n") should include(s"operation=$controllerName.lookupVerified")
            logs should not be empty

            Leakprobes.leakingLines(logs, probe) shouldBe empty
          }
      }
    }
}



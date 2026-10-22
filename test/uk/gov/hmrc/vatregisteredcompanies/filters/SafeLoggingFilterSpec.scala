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

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.typedmap.TypedEntry
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.routing.{HandlerDef, Router}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.config.ControllerConfigs
import uk.gov.hmrc.vatregisteredcompanies.testsupport.{LogCapture, Leakprobes}

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*


class SafeLoggingFilterSpec extends AnyWordSpec with Matchers with LogCapture with BeforeAndAfterAll {
  
  private val system                        = ActorSystem("SafeLoggingFilterSpec")
  private implicit val mat: Materializer    = Materializer(system)
  private implicit val ec: ExecutionContext = ExecutionContext.global
  
  override protected def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  private val filter = new SafeLoggingFilter(ControllerConfigs.fromConfig(Configuration.empty))

  private val controllerName =  "uk.gov.hmrc.vatregisteredcompanies.controllers.VatRegCoLookupController"
  
  private def routedRequest(path: String, method: String, headers: (String, String)*) =
    FakeRequest("GET", path)
      .withHeaders(headers*)
      .addAttrs(
        TypedEntry(
          Router.Attrs.HandlerDef,
          HandlerDef(getClass.getClassLoader, "router", controllerName, method, Seq.empty, "GET", path, "", Seq.empty)
        )
      )
    
  private def run(next: RequestHeader => Future[Result], request: RequestHeader): Unit =
    Await.ready(filter(next)(request), 5.seconds)
  
  private val ok: RequestHeader => Future[Result]         = _ => Future.successful(Results.Ok)
  private val badRequest: RequestHeader => Future[Result] = _ => Future.successful(Results.BadRequest)

    
  "SafeLoggingFilter" should {
    Leakprobes.all.foreach {
      probe =>
        s"log only operation, request id, status and duration for /lookup/:target with ${probe.label}" in {

          val logs = capturedLogs {
            run(ok, routedRequest(s"/vat-registered-companies/lookup/${probe.value}", "lookup", "X-Request-ID" -> "test-123"))
          }

          logs should not be empty
          Leakprobes.leakingLines(logs, probe) shouldBe empty
          val line = logs.find(_.contains("operation=")).getOrElse(fail("no filter log line"))
          line should include("operation=" + controllerName + ".lookup")
          line should include("requestId=test-123")
          line should include("status=200")
          line should include("durationMs=")

        }

        s"record the status of a rejected request without logging ${probe.label}" in {
          val logs = capturedLogs {
            run(badRequest, routedRequest(s"/vat-registered-companies/lookup/${probe.value}", "lookup"))
          }

          logs should not be empty
          Leakprobes.leakingLines(logs, probe) shouldBe empty
          logs.mkString("\n") should include("status=400")
        }


        s"not log target or requester for the verified lookup with ${probe.label}" in {
          val logs = capturedLogs {
            run(ok, routedRequest(s"/vat-registered-companies/lookup/${probe.value}/${probe.value}", "lookupVerified"))
          }

          logs should not be empty
          Leakprobes.leakingLines(logs, probe) shouldBe empty
          logs.mkString("\n") should include("operation=" + controllerName + ".lookupVerified")
        }


        s"not log the path of a request that matches no route with ${probe.label}" in {
          val logs = capturedLogs {
            run(ok, FakeRequest("GET", s"/vat-registered-companies/nothing/${probe.value}"))
          }

          logs should not be empty
          Leakprobes.leakingLines(logs, probe) shouldBe empty
          logs.mkString("\n") should include("operation=unrouted")
        }


        s"log the exception class but not its message when a request fails with ${probe.label}" in {
          val failing: RequestHeader => Future[Result] =
            _ => Future.failed(new RuntimeException(s"RuntimeException ${probe.value}"))

          val logs = capturedLogs {
            run(failing, routedRequest(s"/vat-registered-companies/lookup/${probe.value}", "lookup"))
          }

          logs should not be empty
          Leakprobes.leakingLines(logs, probe) shouldBe empty
          logs.mkString("\n") should include("error=RuntimeException")

        }

    }
  }
  
  "application config" should {

    "replace Bootstrap's LoggingFilter with SafeLoggingFilter" in {
      val conf = ConfigFactory.load()

      conf.getStringList("play.filters.disabled").asScala should contain(
        "uk.gov.hmrc.play.bootstrap.filters.LoggingFilter"
      )
      conf.getStringList("play.filters.enabled").asScala should contain(
        "uk.gov.hmrc.vatregisteredcompanies.filters.SafeLoggingFilter"
      )
    }
  }
}

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

package uk.gov.hmrc.vatregisteredcompanies.controllers

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InboundDataControllerSpec extends WordSpec
  with MockitoSugar
  with Matchers
  with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Map("auditing.enabled" -> "false")).build()

  implicit val materializer = app.materializer
  implicit val configuration = app.configuration
  implicit val environment = Environment.simple()

  val token = app.configuration.getString("mdg.inboundData.token").getOrElse("")

  val fakeHeaders =
    FakeHeaders(
      Seq(
        "Content-type" -> "application/json",
        HeaderNames.AUTHORIZATION -> s"Bearer $token"
      )
    )

  val fakeBody: String = """{
                   |  "createsAndUpdates": [
                   |    {
                   |      "name": "veniam nisi Lorem laboris",
                   |      "address": {
                   |        "line1": "qui ex",
                   |        "countryCode": "au"
                   |      },
                   |      "vatNumber": "993064963231"
                   |    }
                   |  ],
                   |  "deletes": [
                   |    "labore ad cillum",
                   |    "non"
                   |  ]
                   |}""".stripMargin

  val fakeRequest = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeHeaders, fakeBody)

  val mockPersistence: PersistenceService = mock[PersistenceService]

//  "POST of valid json to /vat-registered-companies/vatregistrations" should {
//    "return 200" in {
//      val fr = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeHeaders, fakeBody)
//      when(mockPersistence.processData(ArgumentMatchers.any())).thenReturn(Future(()))
//      val controller = new InboundDataController(mockPersistence)
//      val result: Future[Result] = controller.handle().apply(fr).run()
//      result.map {r =>
//        println(s"####################################### $r")
//      }
//      status(result) shouldBe Status.OK
//    }
//  }

}

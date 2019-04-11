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
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.vatregisteredcompanies.repositories.VatRegisteredCompaniesRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InboundDataControllerSpec extends WordSpec
  with MockitoSugar
  with Matchers
  with GuiceOneAppPerSuite {

  val token = "foobar"
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      Map(
        "auditing.enabled" -> "false",
        "microservice.services.schedulers.old-data-deletion.enabled" -> false,
        "microservice.services.schedulers.payload.conversion.enabled" -> false,
        "microservice.services.mdg.inboundData.token" -> token
      )
    ).build()

  implicit val environment: Environment = Environment.simple()
  implicit val configuration: Configuration = app.configuration

  val fakeHeaders =
    FakeHeaders(
      Seq(
        "Content-type" -> "application/json",
        HeaderNames.AUTHORIZATION -> s"Bearer $token"
      )
    )
  val fakeBadHeaders =
    FakeHeaders(
      Seq(
        "Content-type" -> "application/json",
        HeaderNames.AUTHORIZATION -> s"Bearer barfoo"
      )
    )
  val fakeMissingHeaders =
    FakeHeaders(
      Seq(
        "Content-type" -> "application/json"
      )
    )
  val fakeBody: JsValue = Json.parse("""{
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
                                       |  "deletes": []
                                       |}""".stripMargin)

  val malformedBody: JsValue = Json.parse("""{
                                            |  "createsAndUpdates": [
                                            |    {
                                            |      "name": "",
                                            |      "address": {
                                            |        "line1": "qui ex",
                                            |        "countryCode": "au"
                                            |      },
                                            |      "vatNumber": "993064963231"
                                            |    }
                                            |  ],
                                            |  "deletes": []
                                            |}""".stripMargin)

  val fakeRequest = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeHeaders, fakeBody)
  val fakeInvalidPayloadRequest = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeHeaders, malformedBody)
  val fakeBadRequest = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeBadHeaders, fakeBody)
  val fakeBadRequest2 = FakeRequest("POST", "/vat-registered-companies/vatregistrations", fakeMissingHeaders, fakeBody)
  val mockPersistence = mock[VatRegisteredCompaniesRepository]

  "POST of valid json with valid headers to /vat-registered-companies/vatregistrations" should {
    "return 200" in {
      when(mockPersistence.process(ArgumentMatchers.any())).thenReturn(Future(()))
      val controller = new InboundDataController(mockPersistence)
      val result: Future[Result] = controller.handle().apply(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "POST of invalid json with valid headers to /vat-registered-companies/vatregistrations" should {
    "return 400" in {
      when(mockPersistence.process(ArgumentMatchers.any())).thenReturn(Future(()))
      val controller = new InboundDataController(mockPersistence)
      val result: Future[Result] = controller.handle().apply(fakeInvalidPayloadRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "POST of valid json with invalid bearer token headers to /vat-registered-companies/vatregistrations" should {
    "return 200" in {
      when(mockPersistence.process(ArgumentMatchers.any())).thenReturn(Future(()))
      val controller = new InboundDataController(mockPersistence)
      val result: Future[Result] = controller.handle().apply(fakeBadRequest)
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

  "POST of valid json with missing bearer token headers to /vat-registered-companies/vatregistrations" should {
    "return 200" in {
      when(mockPersistence.process(ArgumentMatchers.any())).thenReturn(Future(()))
      val controller = new InboundDataController(mockPersistence)
      val result: Future[Result] = controller.handle().apply(fakeBadRequest2)
      status(result) shouldBe Status.UNAUTHORIZED
    }
  }

}

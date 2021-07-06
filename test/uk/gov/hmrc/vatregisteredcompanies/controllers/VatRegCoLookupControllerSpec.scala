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

package uk.gov.hmrc.vatregisteredcompanies.controllers

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.vatregisteredcompanies.models._
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegCoLookupControllerSpec extends WordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with MockitoSugar
  with OptionValues
{

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(
      Map(
        "auditing.enabled" -> "false",
        "microservice.services.schedulers.payload.conversion.enabled" -> false,
        "microservice.services.schedulers.old-data-deletion.enabled" -> false
      )
    ).build()

  val mockPersistence: PersistenceService = mock[PersistenceService]
  val mockAudiConnector: AuditConnector = mock[AuditConnector]
  val fakeRequest = FakeRequest("GET", "/lookup/123456789")
  val fakeVerifiedRequest = FakeRequest("GET", "/lookup/123456789/123456789")
  val testVatNo = "123456789"
  val controller = new VatRegCoLookupController(mockPersistence, mockAudiConnector, cc)

  val knownCo =
    VatRegisteredCompany(
      name = "ACME trading",
      vatNumber = testVatNo,
      address = Address("line 1", None, None, None, None, None, countryCode = "GB")
    )

  "GET of unknown VAT number " should {
    "return 200 but an empty target VAT registered company" in {
      when(mockPersistence.lookup(any())).thenReturn(Future(Option.empty[LookupResponse]))
      val result = controller.lookup(any())(fakeRequest)
      status(result) shouldBe Status.OK
      Json.fromJson[LookupResponse](contentAsJson(result)).map { lr =>
        lr.target shouldBe Option.empty[VatNumber]
      }
    }
  }

  "GET of known VAT number " should {
    "return 200 and a target VAT registered company" in {
      when(mockPersistence.lookup(any())).thenReturn(Future(Some(LookupResponse(target = Some(knownCo)))))
      val result = controller.lookup(any())(fakeRequest)
      status(result) shouldBe Status.OK
      Json.fromJson[LookupResponse](contentAsJson(result)).map { lr =>
        lr.target shouldBe Some(knownCo)
      }
    }
  }

  "GET of known VAT number with known requester supplied " should {
    "return 200, a target VAT registered company and a consultation number" in {
      when(mockPersistence.lookup(any())).thenReturn(
        Future(
          Some(
            LookupResponse(
              target = Some(knownCo)
            )
          )
        )
      )
      val result = controller.lookupVerified(testVatNo, testVatNo)(fakeVerifiedRequest)
      status(result) shouldBe Status.OK
      Json.fromJson[LookupResponse](contentAsJson(result)).map { lr =>
        lr.target shouldBe Some(knownCo)
        lr.requester shouldBe Some(testVatNo)
        lr.consultationNumber shouldBe 'defined
      }
    }
  }

  "GET of unknown VAT number with requester supplied " should {
    "return 200, a target VAT registered company and a consultation number" in {
      when(mockPersistence.lookup(any())).thenReturn(
        Future(
          None
        )
      )
      val result = controller.lookupVerified(testVatNo, testVatNo)(fakeVerifiedRequest)
      status(result) shouldBe Status.OK
      Json.fromJson[LookupResponse](contentAsJson(result)).map { lr =>
        lr.target shouldBe Option.empty[VatNumber]
      }
    }
  }

  "GET of known VAT number with unknown requester supplied " should {
    "return 200, a target VAT registered company and no consultation number" in {
      when(mockPersistence.lookup(any())).thenReturn(
        Future(
          Some(
            LookupResponse(
              target = Some(knownCo)
            )
          )
        ),
        Future(
          None
        )
      )
      val result = controller.lookupVerified(testVatNo, testVatNo)(fakeVerifiedRequest)
      status(result) shouldBe Status.OK
      Json.fromJson[LookupResponse](contentAsJson(result)).map { lr =>
        println(lr)
        lr.target shouldBe Some(knownCo)
        lr.requester shouldBe Option.empty[VatNumber]
        lr.consultationNumber shouldBe Option.empty[ConsultationNumber]
      }
    }
  }

}


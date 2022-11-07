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

package uk.gov.hmrc.vatregisteredcompanies.controllers

import play.api.libs.json.Json
import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData._
import uk.gov.hmrc.vatregisteredcompanies.models.LookupResponse

class VatregCoControllerISpec extends IntegrationSpecBase {

  override def beforeEach(): Unit = {
    deleteAll
  }


  val lookupPath = s"/lookup/$testVatNo1"
  val lookupWithRequesterPath = s"/lookup/$testVatNo1/$testVatNo2"

  "GET /lookup/:vatNumber" should {
    "return 200 and a lookup response" that {
      "contains no target" when {
        "there are no records matching the vatNumber" in {
          stubAudit

          val request = buildRequest(lookupPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target.isDefined shouldBe false
          }
        }
      }

      "contains a target that is the payload for the vatNumber" when {
        "there is one record matching the vatNumber" in {
          stubAudit
          insertOne(acmeTradingWithVatNo1)
          val request = buildRequest(lookupPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
          }
        }
      }

      "contains a target that is the newest payload for the vatNumber" when {
        "there is more than one record matching the vatNumber" in {
          stubAudit
          insertOne(getVatRegCompany(testVatNo1, "Acme Trading"))
          Thread.sleep(100)
          insertOne(acmeTradingWithVatNo1)

          val request = buildRequest(lookupPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
          }
        }
      }
    }
  }

  "GET /lookup/:vatNumber/:requester" should {
    "return 200 and a lookup response" that {
      "contains no target or requester" when {
        "there are no records matching the vatNumber or requester vatNumber" in {
          stubAudit
          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target.isDefined shouldBe false
            respBody.requester.isDefined shouldBe false
          }
        }
      }

      "contains a target that is the payload for the vatNumber but no requester" when {
        "there is one record matching the vatNumber and no records for requester vatNumber" in {
          stubAudit
          insertOne(acmeTradingWithVatNo1)
          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
            respBody.requester.isDefined shouldBe false
          }
        }
      }

      "contains a target that is the newest payload for the vatNumber and no requester" when {
        "there is more than one record matching the vatNumber and no records matching requester" in {
          stubAudit
          insertOne(getVatRegCompany(testVatNo1, "Acme Company"))
          Thread.sleep(100)
          insertOne(acmeTradingWithVatNo1)

          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
            respBody.requester.isDefined shouldBe false
          }
        }
      }

      "contains no target but a requester" when {
        "there are no records matching the vatNumber and record for requester vatNumber" in {
          stubAudit
          insertOne(acmeTradingWithVatNo2)
          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target.isDefined shouldBe false
            respBody.requester.isDefined shouldBe true
          }
        }
      }

      "contains a target that is the payload for the vatNumber and a requester" when {
        "there is one record matching the vatNumber and a record for requester vatNumber" in {
          stubAudit
          insertOne(acmeTradingWithVatNo1)
          insertOne(acmeTradingWithVatNo2)
          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
            respBody.requester.isDefined shouldBe true
          }
        }
      }

      "contains a target that is the newest payload for the vatNumber and a requester" when {
        "there is more than one record matching the vatNumber and a record matching requester" in {
          stubAudit
          insertOne(getVatRegCompany(testVatNo1, "Test Company"))
          Thread.sleep(100)
          insertOne(acmeTradingWithVatNo1)
          insertOne(acmeTradingWithVatNo2)

          val request = buildRequest(lookupWithRequesterPath)
            .get()

          whenReady(request) {resp =>
            resp.status shouldBe 200
            val respBody = Json.parse(resp.body).as[LookupResponse]
            respBody.target shouldBe Some(acmeTradingWithVatNo1)
            respBody.requester.isDefined shouldBe true
          }
        }
      }
    }
  }

}

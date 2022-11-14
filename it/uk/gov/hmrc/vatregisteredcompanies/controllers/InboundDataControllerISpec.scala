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

import play.api.http.HeaderNames._
import play.api.libs.json.Json
import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData.{invalidVatNo, testPayloadCreateAndUpdates}
import uk.gov.hmrc.vatregisteredcompanies.models.Payload

class InboundDataControllerISpec extends IntegrationSpecBase {

  val path = "/vatregistrations"


  "POST /vatregistrations" when {
    "an authenticated request is received" that {
      "is schema valid" should {
        "insert the payload in the buffer repository and return 200 with SUCCESS outcome" in {
          stubAudit
          val request = Json.toJson(testPayloadCreateAndUpdates)

          val res =  buildRequest(path)
            .addHttpHeaders(
              (AUTHORIZATION, AUTHORISATION_TOKEN),
              (CONTENT_TYPE, "application/json")
            )
            .post(request)

          whenReady(res) {resp =>
            resp.status shouldBe 200
            val body = Json.parse(resp.body)
            (body \ "outcome").as[String] shouldBe "SUCCESS"
          }
        }
      }

      "is schema invalid" should {
        "not insert the payload in the buffer repository and return 400 with FAILURE outcome and code INVALID_PAYLOAD" in {
          stubAudit
          val request = Json.toJson(Payload(List.empty, List(invalidVatNo)))

          val res =  buildRequest(path)
            .addHttpHeaders(
              (AUTHORIZATION, AUTHORISATION_TOKEN),
              (CONTENT_TYPE, "application/json")
            )
            .post(request)

          whenReady(res) {resp =>
            resp.status shouldBe 400
            val body = Json.parse(resp.body)
            (body \ "outcome").as[String] shouldBe "FAILURE"
            (body \ "code").as[String] shouldBe "INVALID_PAYLOAD"
          }
        }
      }
    }

    "an unauthenticated request is received" should {
      "return unauthorised" in {
        stubAudit
        val request = Json.toJson(testPayloadCreateAndUpdates)

        val res =  buildRequest(path)
          .addHttpHeaders(
            (AUTHORIZATION, "InvalidToken"),
            (CONTENT_TYPE, "application/json")
          )
          .post(request)

        whenReady(res) {resp =>
          resp.status shouldBe 401
          resp.body shouldBe "Supplied bearer token does not match config"
        }
      }
    }
  }

}

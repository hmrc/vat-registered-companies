/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vatregisteredcompanies.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import play.api.libs.json.{JsValue, Json}

class PayloadSubmissionResponseSpec  extends AnyFlatSpec with Matchers {

  "A PayloadSubmissionResponse" should "be serialisable" in {
    val item = new PayloadSubmissionResponse(
      PayloadSubmissionResponse.Outcome.SUCCESS,
      None
    )
    println(Json.toJson(item))
    Json.toJson(item).as[PayloadSubmissionResponse] should be (item)
  }

  "A PayloadSubmissionResponse with an unknown enum value" should "error" in {
    val json: JsValue =
      Json.parse(
      """{"outcome":"FOOBAR","processingDate":"2019-02-19T20:36:14.922"}""".stripMargin
    )
    PayloadSubmissionResponse.backendResponseFormat.reads(json).isError shouldBe true
  }

  "A PayloadSubmissionResponse with an unknown enum type " should "error" in {
    val json: JsValue =
      Json.parse(
        """{"outcome":{"foo":"bar"},"processingDate":"2019-02-19T20:36:14.922"}""".stripMargin
      )
    PayloadSubmissionResponse.backendResponseFormat.reads(json).isError shouldBe true
  }
}

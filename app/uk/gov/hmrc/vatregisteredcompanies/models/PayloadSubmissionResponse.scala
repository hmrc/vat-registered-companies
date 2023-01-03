/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.{OffsetDateTime, ZoneOffset}

import play.api.libs.json.{Json, OFormat}

case class PayloadSubmissionResponse(
  outcome: PayloadSubmissionResponse.Outcome.Value,
  code: Option[PayloadSubmissionResponse.Code.Value],
  processingDate: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
)

object PayloadSubmissionResponse {

  object Outcome extends Enumeration {
    val SUCCESS, FAILURE = Value
    implicit val format = EnumUtils.enumFormat(Outcome)
  }

  object Code extends Enumeration {
    val INVALID_PAYLOAD, SERVER_ERROR = Value
    implicit val format = EnumUtils.enumFormat(Code)
  }

  implicit val backendResponseFormat: OFormat[PayloadSubmissionResponse] =
    Json.format[PayloadSubmissionResponse]
}
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

package uk.gov.hmrc.vatregisteredcompanies

import java.time._

import play.api.libs.json._

import scala.util.Random

package object models {

  type CompanyName = String
  type VatNumber = String
  type ConsultationNumber = String
  type ProcessingDate = OffsetDateTime

  object ConsultationNumber {
    def generate: ConsultationNumber =
      new Random().alphanumeric.filter(x =>
        x.toLower >= 'a' && x.toLower <= 'z'
      ).take(9).toList.mkString
  }

  object ProcessingDate {
    val temporalReads: Reads[OffsetDateTime] = new Reads[OffsetDateTime] {
      override def reads(json: JsValue): JsResult[OffsetDateTime] = {
        (json \ "$date").validate[Long] map (millis =>
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))
      }
    }

    val temporalWrites: Writes[OffsetDateTime] = new Writes[OffsetDateTime] {
      override def writes(o: OffsetDateTime): JsValue = Json.obj("$date" -> Instant.from(o).toEpochMilli)
    }

    implicit val processingDateTimeFormat: Format[ProcessingDate] = Format(temporalReads, temporalWrites)
  }

}

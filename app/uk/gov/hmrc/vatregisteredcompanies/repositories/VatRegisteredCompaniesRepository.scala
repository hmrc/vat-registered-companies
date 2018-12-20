/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatregisteredcompanies.repositories

import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatregisteredcompanies.models.{VatNumber, VatRegisteredCompany}

import scala.concurrent.ExecutionContext

final case class Wrapper(
  vatNumber: VatNumber,
  company: VatRegisteredCompany,
  _id: BSONObjectID = BSONObjectID.generate(),
  timestamp: Instant = Instant.now
)

object Wrapper {
  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def writes(o: Instant): JsValue = {
      Json.toJson(BSONDateTime(o.toEpochMilli))
    }

    override def reads(json: JsValue): JsResult[Instant] = {
      json.validate[BSONDateTime] map { dt => Instant.ofEpochMilli(dt.value) }
    }
  }

  val format: Format[Wrapper] = Json.format[Wrapper]
}

@Singleton
class VatRegisteredCompaniesRepository @Inject()(reactiveMongoComponent: ReactiveMongoComponent)(implicit val executionContext: ExecutionContext)
  extends ReactiveRepository("vatregisteredcompanies", reactiveMongoComponent.mongoConnector.db, Wrapper.format) {

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("timestamp" -> IndexType.Ascending, "vatNumber" -> IndexType.Text),
      unique = true
    )
  )
}

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

package uk.gov.hmrc.vatregisteredcompanies.repositories

import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDateTime
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatregisteredcompanies.models.{Payload, VatNumber, VatRegisteredCompany}

import scala.concurrent.{ExecutionContext, Future}

final case class Wrapper(
  vatNumber: VatNumber,
  company: VatRegisteredCompany
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

  implicit val format: OFormat[Wrapper] = Json.format[Wrapper]

  private def insertOrUpdate(entry: Wrapper): Future[Unit] = {

    domainFormatImplicit.writes(entry) match {
      case a@JsObject(_) =>
        val selector = Json.obj("vatNumber" -> entry.vatNumber)
        collection.update(selector, entry, upsert = true)
      case _ =>
        Future.failed[WriteResult](new Exception("failed insert or update"))
    }
  }.map(_ => ())

  private def insert(entries: List[Wrapper])=
    entries.map(wrapper => insertOrUpdate(wrapper)).head

  private def delete(deletes: List[VatNumber]) =
    deletes.map{ vatNumber =>
      remove("vatNumber" -> vatNumber)
    }.map(_ => Future(())).head

  private def wrap(payload: Payload): List[Wrapper] =
    payload.createsAndUpdates.map { company =>
      Wrapper(company.vatNumber, company)
    }

  // TODO figure out if we need to pass all these Units around
  def process(payload: Payload): Future[(Unit, Unit)] = {
    for {
      insertResult <- insert(wrap(payload))
      deleteResult <- delete(payload.deletes)
    } yield (insertResult, deleteResult)
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq( "vatNumber" -> IndexType.Text),
      unique = true
    )
  )

}

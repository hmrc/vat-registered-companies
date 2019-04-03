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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, _}
import reactivemongo.core.nodeset.ProtocolMetadata
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber, VatRegisteredCompany}

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
class   VatRegisteredCompaniesRepository @Inject()(reactiveMongoComponent: ReactiveMongoComponent)
  (implicit val executionContext: ExecutionContext) extends
  ReactiveRepository("vatregisteredcompanies", reactiveMongoComponent.mongoConnector.db, Wrapper.format) {

  implicit val format: OFormat[Wrapper] = Json.format[Wrapper]

  private def insert(entries: List[Wrapper]): Future[Unit] = {
    bulkInsert(entries).map(_ => (()))
  }

  private def delete(deletes: List[VatNumber]): Future[Unit] = {
    val bulkSize = ProtocolMetadata.Default.maxBulkSize - 1
    val it = deletes.sliding(bulkSize,bulkSize)
    while (it.hasNext) {
      val chunk = it.next
      val deleteBuilder = collection.delete(false)
      val ds: Future[List[collection.DeleteCommand.DeleteElement]] =
        Future.sequence(
          chunk.map { vatNumber =>
            deleteBuilder.element(
              q = BSONDocument("vatNumber" -> vatNumber),
              limit = None,
              collation = None)
          }
        )
      ds.flatMap { ops =>
        deleteBuilder.many(ops)
      }.map {
        multiBulkWriteResult => multiBulkWriteResult.errmsg.foreach( e =>
          Logger.error(s"$e")
        )
      }
    }
    Future.successful((()))
  }

  private def deleteB(deletes: List[BSONValue]): Future[Unit] = {
    val deleteBuilder = collection.delete(false)
    val ds: Future[List[collection.DeleteCommand.DeleteElement]] =
      Future.sequence(
        deletes.map { id =>
          deleteBuilder.element(
            q = BSONDocument("_id" -> id),
            limit = None,
            collation = None)
        }
      )
    ds.flatMap { ops => deleteBuilder.many(ops) }.map(_=> (()))
  }

  private def findOld(n: Int): Future[List[BSONDocument]] = {
    import collection.BatchCommands.AggregationFramework.{Group, Limit, Match, MinField, SumAll}
    collection.aggregatorContext[BSONDocument](
      Group(JsString("$vatNumber"))( "count" -> SumAll, "oldest" -> MinField("_id")),
      List(
        Match(Json.obj("count" -> Json.obj("$gt" -> 1L))),
        Limit(n)
      )
    ).
      prepared.cursor.
      collect[List](-1, Cursor.FailOnError[List[BSONDocument]]())
  }

  def deleteOld(n: Int): Future[Unit] = {
    findOld(n).flatMap(x => deleteB(x.flatMap(y => y.get("oldest"))))
  }

  private def wrap(payload: Payload): List[Wrapper] =
    payload.createsAndUpdates.map { company =>
      Wrapper(company.vatNumber, company)
    }

  def process(payload: Payload): Future[Unit] = {
    val upserts = insert(wrap(payload))
    val deletes = delete(payload.deletes)
    for {
      a <- upserts
      b <- deletes
    } yield (())
  }

  def lookup(target: String): Future[Option[LookupResponse]] = {
    collection
      .find(BSONDocument("vatNumber" -> target), Option.empty[JsObject])
      .sort(Json.obj("_id" -> -1))
      .cursor[Wrapper]()
      .collect[List](1, Cursor.FailOnError[List[Wrapper]]())
      .map {
        _.headOption.map(x => LookupResponse(x.company.some))
      }
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq( "vatNumber" -> IndexType.Text),
      unique = false
    )
  )

}

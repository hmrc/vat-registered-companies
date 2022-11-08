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

package uk.gov.hmrc.vatregisteredcompanies.helpers

import play.api.libs.json.{JsObject, Json}
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.vatregisteredcompanies.models.VatRegisteredCompany
import uk.gov.hmrc.vatregisteredcompanies.repositories.{VatRegisteredCompaniesRepository, Wrapper}

trait VatRegisteredCompaniesDatabaseOperations {

  self: IntegrationSpecBase =>

  val vatRegisteredCompaniesRepository: VatRegisteredCompaniesRepository

  def insertOne(vatRegisteredCompany: VatRegisteredCompany): Boolean = {
    val wrapper = Wrapper(vatRegisteredCompany.vatNumber, vatRegisteredCompany)
    await(
      vatRegisteredCompaniesRepository.insert(wrapper).map(_.ok)
    )
  }

  def insertMany(vatRegisteredCompanyList: List[VatRegisteredCompany]): Boolean = {
    val wrappers = vatRegisteredCompanyList
      .map(vatRegisteredCompany => Wrapper(vatRegisteredCompany.vatNumber, vatRegisteredCompany))
    await(
      vatRegisteredCompaniesRepository.bulkInsert(wrappers).map(_.ok)
    )
  }

  def totalCount: Int = {
    await(vatRegisteredCompaniesRepository.count)
  }

  //  def getRecords: List[VatRegisteredCompany] = {
  //    await(
  //      vatRegisteredCompaniesRepository
  //        .collection
  //      .find(BSONDocument(), Option.empty[JsObject])
  //      .sort(Json.obj("_id" -> 1))
  //      .cursor[VatRegisteredCompany]()
  //      .collect[List](100, Cursor.FailOnError[List[VatRegisteredCompany]]()))
  //  }

  def deleteAll: Boolean = {
    await(vatRegisteredCompaniesRepository.removeAll().map(_.ok))
  }
}

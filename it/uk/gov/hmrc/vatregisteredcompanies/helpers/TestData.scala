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

import uk.gov.hmrc.vatregisteredcompanies.models.{Address, VatNumber, VatRegisteredCompany}

object TestData {

  val testVatNo1 = "123456789"
  val testVatNo2 = "223456789"
  val testVatNo3 = "323456789"

  def getVatRegCompany(vatNumber: String) =
    VatRegisteredCompany(
      name = "ACME trading",
      vatNumber = vatNumber,
      address = Address("line 1", None, None, None, None, None, countryCode = "GB")
    )

}

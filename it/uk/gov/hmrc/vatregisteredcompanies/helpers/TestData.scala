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

import uk.gov.hmrc.vatregisteredcompanies.models.{Address, VatRegisteredCompany, Payload}
import uk.gov.hmrc.vatregisteredcompanies.repositories.PayloadWrapper

object TestData {
  val testVatNo1 = "123456789"
  val testVatNo2 = "223456789"
  val testVatNo3 = "323456789"

  val vatRegisteredCompany1: VatRegisteredCompany = getVatRegCompany(testVatNo1)
  val vatRegisteredCompany2: VatRegisteredCompany = getVatRegCompany(testVatNo2)
  val vatRegisteredCompany3: VatRegisteredCompany = getVatRegCompany(testVatNo3)

  val testPayloadCreateAndUpdates: Payload = Payload(List(vatRegisteredCompany1, vatRegisteredCompany2), List())
  val testPayloadDeletes: Payload = Payload(List(), List(testVatNo1, testVatNo2))

  val testLockId = 2
  val testLockId2 = 123

  def getVatRegCompany(vatNumber: String): VatRegisteredCompany =
    VatRegisteredCompany(
      name = "ACME trading",
      vatNumber = vatNumber,
      address = Address("line 1", None, None, None, None, None, countryCode = "GB")
    )
}

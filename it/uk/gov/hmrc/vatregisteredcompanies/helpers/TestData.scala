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

import uk.gov.hmrc.vatregisteredcompanies.models.{Address, Payload, VatRegisteredCompany}
import uk.gov.hmrc.vatregisteredcompanies.repositories.Lock
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

object TestData {
  val testVatNo1 = "123456789"
  val testVatNo2 = "223456789"
  val testVatNo3 = "323456789"
  val testVatNo4 = "423456780"

  val vatRegisteredCompany1: VatRegisteredCompany = getVatRegCompany(testVatNo1)
  val vatRegisteredCompany2: VatRegisteredCompany = getVatRegCompany1(testVatNo2)
  val vatRegisteredCompany3: VatRegisteredCompany = getVatRegCompany2(testVatNo3)
  val vatRegisteredCompany4: VatRegisteredCompany = getVatRegCompany(testVatNo4)

  val testPayloadCreateAndUpdates1: Payload = Payload(List(vatRegisteredCompany3, vatRegisteredCompany4), List())
  val testPayloadCreateAndUpdates: Payload = Payload(List(vatRegisteredCompany1, vatRegisteredCompany2), List())
  val testPayloadDeletes: Payload = Payload(List(), List(testVatNo1, testVatNo2))

  val testLockId = 1234
  val testLock: Lock = Lock(testLockId)

  val formattedDateString = "2020-07-01T00:00:00Z"
  val pastTime: LocalDateTime = LocalDateTime.parse(
    formattedDateString,
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
  );
  val expiredTestLock: Lock = Lock(testLockId, pastTime)

  def getVatRegCompany(vatNumber: String): VatRegisteredCompany =
    VatRegisteredCompany(
      name = "ACME trading",
      vatNumber = vatNumber,
      address = Address("line 1", None, None, None, None, None, countryCode = "GB")
    )

  def getVatRegCompany1(vatNumber: String): VatRegisteredCompany =
    VatRegisteredCompany(
      name = "Delta trading",
      vatNumber = vatNumber,
      address = Address("c/o Delta trading", None, None, None, None, None, countryCode = "GB")
    )

  def getVatRegCompany2(vatNumber: String): VatRegisteredCompany =
    VatRegisteredCompany(
      name = "Alpha trading",
      vatNumber = vatNumber,
      address = Address("c/o Alpha trading co", None, None, None, None, None, countryCode = "GB")
    )
}

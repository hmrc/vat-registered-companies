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

package uk.gov.hmrc.vatregisteredcompanies.repository

import org.mongodb.scala.{MongoCommandException, SingleObservableFuture}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData.*
import uk.gov.hmrc.vatregisteredcompanies.repositories.PayloadWrapper

class VatRegisteredCompaniesRepositoryISpec extends IntegrationSpecBase {

  def allowDuplicates(): Unit = {
    await(vatRegisteredCompaniesRepository.collection.dropIndex("vatNumber_prepare_unique_idx").toFuture().map(_ => ()).recover {
      case e: MongoCommandException if e.getErrorCode == 27 => ()
    }
    )
  }

  override def beforeEach(): Unit = {
    deleteAll()
    await(vatRegisteredCompaniesRepository.applyPrepareUnique())
  }

  "Method: process" when {
    "the payload contains only create and updates" should {
      "return unit and insert into database" when {

        "the database has no records" in {
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 2
          }
        }

        "the database has one record" in {
          insertOne(acmeTradingWithVatNo3)
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 3
          }
        }

        "the database has multiple records, including duplicate VAT number" in {
          insertOne(acmeTradingWithVatNo1)
          insertOne(acmeTradingWithVatNo3)
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val result = vatRegisteredCompaniesRepository.process(payload)
          whenReady(result) { res =>
            res shouldBe ((): Unit)
            totalCount shouldBe 3
          }
        }

        "produce the same database state when the same payload is processed twice" in {
          val payload = PayloadWrapper(payload = testPayloadCreateAndUpdates)
          val firstResult = vatRegisteredCompaniesRepository.process(payload)
          whenReady(firstResult) { first =>
            first shouldBe ((): Unit)
            totalCount shouldBe 2

            val secondResult = vatRegisteredCompaniesRepository.process(payload)
            whenReady(secondResult) { second =>
              second shouldBe ((): Unit)
              totalCount shouldBe 2
            }
          }
        }

      }
    }
  }

  "Method: lookup" should {
    "return the latest lookup response with the vatRegisteredCompany" when {
      "the database has no records" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)
        whenReady(result) { res =>
          res shouldBe None
        }
      }

      "the database has a record with the matching vatNumber" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        insertOne(acmeTradingWithVatNo1)
        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)

        whenReady(result) { res =>
          res shouldBe defined
          res.get.target shouldBe defined
          res.get.target.get.vatNumber shouldBe testVatNo1
        }
      }

      "the database has more than one record with the vatNumber" in {
        allowDuplicates()
        val oldestRecord = acmeTradingWithVatNo1
        val newestRecord = oldestRecord.copy(name = "newCompany")
        insertOne(oldestRecord)
        Thread.sleep(100)
        insertOne(newestRecord)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = vatRegisteredCompaniesRepository.lookup(testVatNo1)
        whenReady(result) {res =>
          res shouldBe defined
          res.get.target shouldBe defined
          res.get.target.get.name shouldBe "newCompany"
        }
      }
    }
  }
}

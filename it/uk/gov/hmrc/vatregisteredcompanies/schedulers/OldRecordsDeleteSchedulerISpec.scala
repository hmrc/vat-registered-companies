package uk.gov.hmrc.vatregisteredcompanies.schedulers

import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData.{acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3, deltaTradingWithVatNo1, deltaTradingWithVatNo2, expiredTestLock, testLock, testVatNo1, testVatNo2}

class OldRecordsDeleteSchedulerISpec  extends IntegrationSpecBase {

  val limit = 2

  override def beforeEach(): Unit = {
    deleteAll
    clearLock()
  }

  "deleteOld with limit 2" when {
    "the lock is not already acquired" should {
      "not remove any documents" when {
        "there are no records with repeated vatNumbers" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2))
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check no records deleted
            totalCount shouldBe 2
            //check lock has been removed
            isLocked shouldBe false
          }
        }
      }
      "remove the oldest record(s)" when {
        "there is one VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          val res = persistenceService.deleteOld(limit)

          whenReady(res) { result =>
            result shouldBe ((): Unit)
            val record = getRecord(testVatNo1)
            record shouldBe defined
            record.get.name shouldBe "Delta Trading"
            totalCount shouldBe 3
            //check lock has been removed
            isLocked shouldBe false
          }
        }

        "there are two VatNumber with more than one record associated with" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, deltaTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 2 of the vatNumbers used above but slightly different details
          insertMany(List(deltaTradingWithVatNo1, acmeTradingWithVatNo2))

          val res = persistenceService.deleteOld(limit)
          whenReady(res) { result =>
            result shouldBe ((): Unit)
            //check only 1 record exists with VatNumber and is the newest for both the repeated vatNumbers
            val record1 = getRecord(testVatNo1)
            record1 shouldBe defined
            record1.get.name shouldBe "Delta Trading"
            val record2 = getRecord(testVatNo2)
            record2 shouldBe defined
            record2.get.name shouldBe "ACME Trading"
            //check lock has been removed
            isLocked shouldBe false
          }
        }
      }

      "remove only two of the oldest record(s)" when {
        "there are three VatNumber with more than one record associated with and the limit is 2" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies two records with 3 of the vatNumbers used above but slightly different details
          insertMany(List(deltaTradingWithVatNo1, deltaTradingWithVatNo2))

          val res = persistenceService.deleteOld(limit)
          whenReady(res) { result =>
            result shouldBe ((): Unit)
            totalCount shouldBe 3
            //check only 2 records deleted
            isLocked shouldBe false
            //check lock has been removed
          }
        }
      }
    }

    "the lock is already acquired" when {
      "it is within the TTL" should {
        "not run the deleteOld job and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          totalCount shouldBe 4
          // insert a lock within TTL
          insert(testLock)
          val res = persistenceService.deleteOld(limit)

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records deleted from vatRegisteredCompanies database
            totalCount shouldBe 4 // Fails but should pass if nothing has been deleted - currently deletes old record
            //check lock is still present
            isLocked shouldBe true
          }
        }
      }

      "it is outside of the TTL" should {
        "not run the deleteOld job, remove the lock and return unit" in {
          //insert into vatRegisteredCompanies records with different vatNumbers
          insertMany(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2, acmeTradingWithVatNo3))
          Thread.sleep(100)
          //insert into vatRegisteredCompanies a record with a vatNumber used above but slightly different details
          insertOne(deltaTradingWithVatNo1)
          // insert a lock outside the TTL
          insert(expiredTestLock)
          val res = persistenceService.deleteOld(limit)
          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records deleted from vatRegisteredCompanies database
            totalCount shouldBe 4
            //check lock has been removed
            isLocked shouldBe false
          }
        }
      }
    }
  }

}

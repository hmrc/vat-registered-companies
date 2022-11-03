package uk.gov.hmrc.vatregisteredcompanies.schedulers

import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase

class OldRecordsDeleteSchedulerISpec extends IntegrationSpecBase {

  "deleteOld" when {
    "the lock is not already acquired" should {
      "remove records that have " when {
        "there is one record in the buffer and no records in vatRegisteredCompanies" that {
          "contains a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }
        }

        "there is one record in the buffer" that {
          "contains a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
             val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            // insert records into vatRegisteredCompanies with at least one with the same vatNumber in the buffer paylod
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted
              //check lock has been removed
            }
          }
        }

        "there is multiple records in the buffer" that {
          "has the oldest record containing a payload with only createsAndUpdates" in {
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            //insert one record into buffer repository that has a payload with only deletes
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check buffer record with payload containing only deletes is deleted
              //check lock has been removed
            }
          }

          "has a oldest record containing a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            // insert records in vatRegisteredCompanies to be deleted
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted with payload containing only deletes
              //check lock has been removed
            }
          }

          "has a oldest record containing a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            //insert one record into buffer repository that has a payload with only deletes
            //insert one record into buffer repository that has a payload with only createsAndUpdates
            //insert records in vatRegisteredCompanies to be deleted
            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              //check buffer record deleted with payload containing createsAndUpdates and deletes
              //check lock has been removed
            }
          }
        }
      }
    }

    "the lock is already acquired" that {
      "is within the TTL" should {
        "not run the processing job and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          //insert records in vatRegisteredCompanies to be deleted
          // insert a lock within TTL
          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted into vatRegisteredCompanies database
            //check no records deleted from vatRegisteredCompanies database
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            //check lock is still present removed
          }

        }
      }

      "is outside of the TTL" should {
        "not run the processing job, remove the lock and return unit" in {
          //insert one record into buffer repository that has a payload with createAndUpdates and deletes
          //insert records in vatRegisteredCompanies to be deleted
          // insert a lock within TTL
          val res = persistenceService.processOneData

          whenReady(res) {result =>
            result shouldBe ((): Unit)
            //check no records inserted into vatRegisteredCompanies database
            //check no records deleted from vatRegisteredCompanies database
            //check buffer record is still present with payload containing createsAndUpdates and deletes
            //check lock has been removed
          }
        }
      }
    }
  }
}

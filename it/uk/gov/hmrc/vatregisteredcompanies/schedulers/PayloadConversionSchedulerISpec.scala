package uk.gov.hmrc.vatregisteredcompanies.schedulers

import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData._

class PayloadConversionSchedulerISpec extends IntegrationSpecBase {

  "processOneData" when {
    "the lock is not already acquired" should {
      "get the oldest payload from the buffer repository and process it then delete buffer record" when {
        "there is one record in the buffer and no records in vatRegisteredCompanies" that {
          "contains a payload with only createsAndUpdates" in {

            //insert one record into buffer repository that has a payload with only createsAndUpdates
            insertOneBuffer(testPayloadCreateAndUpdates)
            bufferTotalCount shouldBe 1
            totalCount shouldBe 0

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)

              //check records inserted into vatRegisteredCompanies database
              totalCount shouldBe 2
              //check buffer record deleted
              bufferTotalCount shouldBe 0
              //check lock has been removed
              lockCount shouldBe 0
            }
          }

          "contains a payload with only deletes" in {
            //insert one record into buffer repository that has a payload with only deletes
            insertOneBuffer(testPayloadDeletes)
            bufferTotalCount shouldBe 1


            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records deleted from vatRegisteredCompanies database
              //TODO: based on above request, no records would be present. - Will have another dev confirm assumption
              //check buffer record deleted
              //check lock has been removed
              bufferTotalCount shouldBe 0
              totalCount shouldBe 0
              lockCount shouldBe 0
            }
          }

          "contains a payload with both createsAndUpdates and deletes" in {
            //insert one record into buffer repository that has a payload with createAndUpdates and deletes
            bufferTotalCount shouldBe 0
            insertOneBuffer(testPayloadCreateAndDeletes)
                //this inserts companies with vatNo1 and vatNo2
                //deletes is deleting vatNo1 and vatNo3

            bufferTotalCount shouldBe(1)
            totalCount shouldBe(0)

            val payloadSentToBufferRepo = await(payloadBufferRepository.list)
            payloadSentToBufferRepo.mkString should include (testVatNo1)
            payloadSentToBufferRepo.mkString should include (testVatNo2)
            // payloadSentToBufferRepo.head.payload should include(testPayloadCreateAndUpdates.toString)

            val res = persistenceService.processOneData

            whenReady(res) {result =>
              result shouldBe ((): Unit)
              //check records inserted into vatRegisteredCompanies database
              //check records deleted from vatRegisteredCompanies database
              totalCount shouldBe(1)
              val secondCompanyStillPresent = await(vatRegisteredCompaniesRepository.findAll())
              secondCompanyStillPresent.mkString shouldNot  include (testVatNo1)
              secondCompanyStillPresent.mkString should include (testVatNo2)
              //check buffer record deleted
              bufferTotalCount shouldBe(0)
              //check lock has been removed
              lockCount shouldBe(0)
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

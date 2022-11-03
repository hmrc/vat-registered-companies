package uk.gov.hmrc.vatregisteredcompanies.helpers
import uk.gov.hmrc.vatregisteredcompanies.repositories.LockRepository

trait LockDatabaseOperations {

  self: IntegrationSpecBase =>

  val lockRepository: LockRepository

  def insert(id: Int): Unit = {
    lockRepository.insert(id)
  }

  def clearLock(): Unit = {
    lockRepository.deleteLock()
  }
}

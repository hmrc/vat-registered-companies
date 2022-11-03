package uk.gov.hmrc.vatregisteredcompanies.helpers
import uk.gov.hmrc.vatregisteredcompanies.repositories.{Lock, LockRepository}

trait LockDatabaseOperations {

  self: IntegrationSpecBase =>

  val lockRepository: LockRepository

  def insert(lock: Lock): Unit = {
    await(lockRepository.collection.insert(true).one(lock).map(_.ok))
  }

  def clearLock(): Unit = {
    await(lockRepository.removeAll().map(_.ok))
  }
}

import uk.gov.hmrc.vatregisteredcompanies.models.{Address, Payload, VatRegisteredCompany}
import uk.gov.hmrc.vatregisteredcompanies.repositories.Lock
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

object TestData {
  val testVatNo1 = "123456789012"
  val testVatNo2 = "223456789012"
  val testVatNo3 = "323456789012"
  val testVatNo4 = "423456780012"
  val invalidVatNo = "hakfgjdfjk"

  val acmeTradingWithVatNo1: VatRegisteredCompany = getVatRegCompany(testVatNo1, "ACME Trading")
  val acmeTradingWithVatNo2: VatRegisteredCompany = getVatRegCompany(testVatNo2, "ACME Trading")
  val acmeTradingWithVatNo3: VatRegisteredCompany = getVatRegCompany(testVatNo3, "ACME Trading")
  val acmeTradingWithVatNo4: VatRegisteredCompany = getVatRegCompany(testVatNo4, "ACME Trading")

  val deltaTradingWithVatNo1: VatRegisteredCompany = getVatRegCompany(testVatNo1, "Delta Trading")

  val testPayloadCreateAndUpdates1: Payload = Payload(List(acmeTradingWithVatNo3, acmeTradingWithVatNo4), List())
  val testPayloadCreateAndUpdates: Payload = Payload(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2), List())
  val testPayloadDeletes: Payload = Payload(List(), List(testVatNo1, testVatNo2))
  val testPayloadDeletes1: Payload = Payload(List(), List(testVatNo1))

  val testPayloadCreateAndDeletes: Payload = Payload(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2), List(testVatNo1, testVatNo3))
  val testPayloadCreateAndDeletes1: Payload = Payload(List(acmeTradingWithVatNo1, acmeTradingWithVatNo2), List(testVatNo2, testVatNo3))


  val testLockId = 1234
  val testLock: Lock = Lock(testLockId)
  val formattedDateString = "2020-07-01T00:00:00Z"
  val pastTime: LocalDateTime = LocalDateTime.parse(
    formattedDateString,
    DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
  );
  val expiredTestLock: Lock = Lock(testLockId, pastTime)

  def getVatRegCompany(vatNumber: String, companyName: String): VatRegisteredCompany =
    VatRegisteredCompany(
      name = companyName,
      vatNumber = vatNumber,
      address = Address("line 1", None, None, None, None, None, countryCode = "GB")
    )
}

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CandlestickManagerImplTest {

  private val ISIN: String = "XE0866287E25"

  private val candlestickManagerService = CandlestickManagerImpl()

  @Test
  fun whenGetCandlesticksCalled_shouldReturnCandlestickList() {

    val priceList:List<Price> = listOf(1654.0, 1687.5, 1690.0, 1680.5,1686.0, 1624.5, 1689.0, 1663.5, 1638.0, 1647.5)
    val timeList: List<Instant> = listOf(
      Instant.now().minusSeconds(300L),
      Instant.now().minusSeconds(150L),
      Instant.now().minusSeconds(100L),
      Instant.now().minusSeconds(75L),
      Instant.now().minusSeconds(70L),
      Instant.now().minusSeconds(60L),
      Instant.now().minusSeconds(55L),
      Instant.now().minusSeconds(45L),
      Instant.now().minusSeconds(20L),
      Instant.now().minusSeconds(10L),
    )

    QueueManager.createTopic(ISIN)

    for (index in priceList.indices)
      QueueManager.pushMessage(ISIN, SocketData(priceList[index], timeList[index]))

    val result: List<Candlestick> = candlestickManagerService.getCandlesticks(ISIN)

    assertNotNull(result)
    assertEquals(QueueManager.getMessage(ISIN)!!.peek().time, result[0].openTimestamp)
    assertEquals(QueueManager.getMessage(ISIN)!!.peek().price, result[0].openPrice)
  }
}

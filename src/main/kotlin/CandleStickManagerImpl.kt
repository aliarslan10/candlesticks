import java.time.Instant
import java.time.ZoneOffset

/**
 * Example data coming from socket:
 * [SocketData(price=341.8169, time=2021-11-19T22:39:03.680425Z)]
 *
 * Each candlestick should include values of following variables:
 *
 * openPrice : the first quotes price, that was received
 * closePrice: the last quotes, that was received
 * highPrice : the highest quote price that was observed
 * lowPrice  : the lowest quote price that was observed
 * openTimestamp : the timestamp when the candlestick was opened
 * closeTimestamp: he timestamp when the candlestick was closed
 */
class CandlestickManagerImpl : CandlestickManager {
  override fun getCandlesticks(isin: String): List<Candlestick> {

    if (QueueManager.getMessage(isin).isNullOrEmpty()) {
      return listOf()
    }

    val socketDataArray: Array<SocketData> = QueueManager.getMessage(isin)!!.toTypedArray()
    val candlestickArray: ArrayList<Candlestick> = ArrayList()

    var currentMinute: Int
    var previousMinute: Int = Int.MIN_VALUE

    var openPrice: Price = socketDataArray[0].price
    var closePrice: Price
    var highPrice: Price = Double.MIN_VALUE
    var lowPrice: Price = Double.MAX_VALUE
    var openTimestamp: Instant = socketDataArray[0].time
    var closeTimestamp: Instant

    socketDataArray.forEachIndexed { index, (price, time) ->
      currentMinute = time.atZone(ZoneOffset.UTC).minute

      // OPEN CONDITION
      if (currentMinute > previousMinute) {
        previousMinute = currentMinute
        openTimestamp = time
        openPrice = price
      }

      lowPrice = lowPrice.coerceAtMost(price)
      highPrice = highPrice.coerceAtLeast(price)
      closeTimestamp = time
      closePrice = price

      // CLOSE CONDITIONS
      if (socketDataArray.size - 1 == index) {
        candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
        var timeDiff: Int = Instant.now().atZone(ZoneOffset.UTC).minute - currentMinute
        while (timeDiff > 1) { // complete missing candlesticks
          openTimestamp = openTimestamp.plusSeconds(60)
          closeTimestamp= closeTimestamp.plusSeconds(60)
          candlestickArray.add(Candlestick(openTimestamp.plusSeconds(60), closeTimestamp.plusSeconds(60), openPrice, highPrice, lowPrice, closePrice))
          timeDiff--
        }
      } else if (socketDataArray[index+1].time.atZone(ZoneOffset.UTC).minute > currentMinute) {
        var timeDiff: Int = socketDataArray[index+1].time.atZone(ZoneOffset.UTC).minute - currentMinute
        candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
        while (timeDiff > 1) { // complete missing candlesticks
          openTimestamp = openTimestamp.plusSeconds(60)
          closeTimestamp= closeTimestamp.plusSeconds(60)
          candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
          timeDiff--
        }
        lowPrice = Double.MAX_VALUE
        highPrice= Double.MIN_VALUE
      }
    }

    return candlestickArray
  }
}
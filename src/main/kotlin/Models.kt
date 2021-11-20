import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayList

data class InstrumentEvent(val type: Type, val data: Instrument) {
  enum class Type {
    ADD,
    DELETE
  }
}

data class QuoteEvent(val data: Quote)

data class Instrument(val isin: ISIN, val description: String)
typealias ISIN = String

data class Quote(val isin: ISIN, val price: Price)
typealias Price = Double

interface CandlestickManager {
  fun getCandlesticks(isin: String): List<Candlestick>
}

class CandlestickManagerImpl : CandlestickManager {
  override fun getCandlesticks(isin: String): List<Candlestick> {
    /**
     * [SocketData(price=341.8169, time=2021-11-19T22:39:03.680425Z)]
     *
     * HER BİR MUM İÇİN !!!!!
     *
     * openPrice : yeni başlayan dakikanın ilk price değeri
     * closePrice: biten dakikanın kapanış değeri
     * highPrice : ilgili dakika içindeki en yüksek değer
     * lowPrice  : ilgili dakika içindeki en düşük değer
     * openTimestamp : yeni dakikanın ilk saniyeleri
     * closeTimestamp: yeni dakikanın son saniyeleri
     */

    val array: Array<SocketData> = QueueManager.getMessage(isin)!!.toTypedArray();
    val candlestickArray: ArrayList<Candlestick> = ArrayList<Candlestick>()

    var currentMinute: Int
    var previousMinute: Int = Int.MIN_VALUE

    var openPrice: Price = array[0].price
    var closePrice: Price= array[0].price
    var highPrice: Price = Double.MIN_VALUE
    var lowPrice: Price = Double.MAX_VALUE
    var openTimestamp: Instant = array[0].time
    var closeTimestamp: Instant = array[0].time

    /**
     * candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
     */

    array.forEachIndexed { index, (price, time) ->
      currentMinute = time.atZone(ZoneOffset.UTC).minute

      // OPEN CONDITION
      if (currentMinute > previousMinute) {
        previousMinute = currentMinute;
        openTimestamp = time
        openPrice = price
      }

      lowPrice = Math.min(lowPrice, price)
      highPrice = Math.max(highPrice, price)
      closeTimestamp = time
      closePrice = price

      // CLOSE CONDITION
      if (array.size - 1 == index) {
        candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
      } else if (array[index+1].time.atZone(ZoneOffset.UTC).minute > currentMinute) {
        candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice))
        lowPrice = Double.MAX_VALUE
        highPrice= Double.MIN_VALUE
      }
    }

    //println(Instant.now())
    println(QueueManager.getMessage(isin))

    return candlestickArray
  }
}

data class Candlestick(
val openTimestamp: Instant,
var closeTimestamp: Instant,
val openPrice: Price,
var highPrice: Price,
var lowPrice: Price,
var closingPrice: Price
)

data class SocketData (
  val price: Price,
  val time: Instant
)
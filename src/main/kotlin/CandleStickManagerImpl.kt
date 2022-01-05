import java.time.Instant
import java.time.ZoneOffset

/**
 * This implementation takes "ISIN" parameter to take all prices from the queue and returns list of the Candlesticks object.
 *
 * ISIN is unique name of the instruments which is coming from 3rd part application over the socket.
 * All instruments that coming from socket is being kept in the queue by QueueManager for 30 minutes.
 * By the ISIN parameter this implementation gets prices of the instruments from the queue and process them.
 *
 * Example data coming from QueueManager :
 * ----------------------------------------
 * [SocketData(price=341.8169, time=2021-11-19T22:39:03.680425Z)]
 *
 * After consumed all prices from queue topic by ISIN parameter, this implementation groups all data in one minutes chunks.
 * While grouping data in one minute chunks, it cares some specific data to create Candlestick object.
 *
 * Each candlestick object should include values of following variables:
 *
 * openPrice    : the first quotes price, that was received
 * closingPrice : the last quotes, that was received
 * highPrice    : the highest quote price that was observed
 * lowPrice     : the lowest quote price that was observed
 * openTimestamp: the timestamp when the candlestick was opened
 * closeTimestamp: he timestamp when the candlestick was closed
 */
class CandlestickManagerImpl : CandlestickManager {


  /**
   * This function represents list of the candlesticks objects for given ISIN to "/candlesticks" endpoint.
   * In this functions firstly, all data copied to Array for related ISIN. Aim of this copying process, reducing the
   * O(n) complexity of the code. Also, prevents to keeping busy the queue. Then this array iterating to grouping
   * data in one minute chunk to create candlestick.
   *
   * If current iteration time is bigger than previous data time, we can assume that we should start to create new candle and
   * code start to sets `openTimestamp` and and `openPrice`
   *
   * To find higher price in current minute, coerceAtMost() is used
   * To find lower price in current minute, coerceAtLeast() is used
   *
   * To creating Candlestick object we need to ensure when we will get the last item of the current minute,
   * code is comparing time of current data time and time of the next data in the array. if time of the next data bigger
   * than current iterating data, then code finally create candlestick for the related minute.
   *
   * Also, if next item is null, we can say we have last item of the array and we should create the candlestick.
   *
   * Example candlestick object :
   * ------------------------------
   * {Candlestick@2728} Candlestick(openTimestamp=2021-11-21T17:26:18.245744Z, closeTimestamp=2021-11-21T17:26:18.245744Z,
   * openPrice=1654.0, highPrice=1654.0, lowPrice=1654.0, closingPrice=1654.0)}
   *
   * If there is not any data in the queue for the given ISIN or there is no topic name for requested isin,
   * then the function returns empty list.
   *
   * @param String 'isin' is unique identifier of the instruments which is coming from socket
   * @return List<CandleSticks> returns lists of candlesticks objects
   */
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

      // OPEN CONDITION of the candlestick
      if (currentMinute > previousMinute) {
        previousMinute = currentMinute
        openTimestamp = time
        openPrice = price
      }

      lowPrice = lowPrice.coerceAtMost(price)
      highPrice = highPrice.coerceAtLeast(price)
      closeTimestamp = time
      closePrice = price

      // CLOSE CONDITIONS of the candlestick
      if (socketDataArray.size - 1 == index) { // checks if this is last data of the array

        val candlestick = Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice)
        candlestickArray.add(candlestick)

        val minuteDifference = Instant.now().atZone(ZoneOffset.UTC).minute - currentMinute
        completeMissingCandlesticks(minuteDifference, candlestick, candlestickArray)

      } else if (socketDataArray[index+1].time.atZone(ZoneOffset.UTC).minute > currentMinute) { // checks this is last data of the current minute of the time

        val candlestick = Candlestick(openTimestamp, closeTimestamp, openPrice, highPrice, lowPrice, closePrice)
        candlestickArray.add(candlestick)

        val minuteDifference = socketDataArray[index+1].time.atZone(ZoneOffset.UTC).minute - currentMinute
        completeMissingCandlesticks(minuteDifference, candlestick, candlestickArray)

        lowPrice = Double.MAX_VALUE
        highPrice= Double.MIN_VALUE
      }
    }

    return candlestickArray
  }

  /**
   * This functions get minute differences between timestamps and it creates new Candlesticks object to adds them
   * to candlestickArray. It prevents to having missing candlesticks.
   *
   * For example we got data from socket (queue) in 10:15, then we got second data in 10:18
   * There are missing datas for 10:16 and 10:17
   * In this situation, this functions create new candlestick objects for time 10:16 and 10:17 with same data of 10:15
   *
   * @param timeDiff Int minute differences between two timestamp
   * @param candlestick candlestick object
   * @param candlestickArray list of the candlestick object that we create for the endpoint
    */
  private fun completeMissingCandlesticks(timeDiff: Int, candlestick: Candlestick, candlestickArray: ArrayList<Candlestick>) {
      var minuteDifferences = timeDiff
      var openTimestamp = candlestick.openTimestamp
      var closeTimestamp= candlestick.closeTimestamp
      while (minuteDifferences > 1) {
        openTimestamp = openTimestamp.plusSeconds(60)
        closeTimestamp= closeTimestamp.plusSeconds(60)
        candlestickArray.add(Candlestick(openTimestamp, closeTimestamp, candlestick.openPrice, candlestick.highPrice, candlestick.lowPrice, candlestick.closingPrice))
        minuteDifferences--
    }
  }
}
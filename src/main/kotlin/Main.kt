import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

fun main() {
  println("starting up")

  val server = Server()
  val instrumentStream = InstrumentStream()
  val quoteStream = QuoteStream()

  /**
   * Consume instruments name from the socket. If type of the data is ADD, function creates empty topic
   * in the QueueManager. If type of the data is DELETE, functions removes topic from the QueueManager.
   * Instrument name is ISIN. And each ISIN is also topic name of the queue.
   *
   * Example instrument data coming from socket:
   * -------------------------
   * InstrumentEvent(type=ADD, data=Instrument(isin=SJ3124P43460, description=commune fuisset ac tristique ex maiestatis))
   *
   * @param InstrumentEvent coming from 3rd party application over socket it consist instrument name (isin) and description.
   */
  instrumentStream.connect { event ->
    if (event.type == InstrumentEvent.Type.ADD)
      QueueManager.createTopic(event.data.isin)
    if (event.type == InstrumentEvent.Type.DELETE)
      QueueManager.removeTopic(event.data.isin)
    println(QueueManager.getTopics())
  }

  /**
   * Consume quotes from the socket and push them to related topic by the ISIN name.
   * Each ISIN is also topic name of the queue. SocketData is being added as queue message of the topic.
   * SocketData involves PRICE, which is coming from socket, and coming time with Instant.now() function.
   *
   * Example Quote data coming from socket :
   * -------------------------
   * QuoteEvent(data=Quote(isin=IA5E35112712, price=1073.3978))
   *
   * Example SocketData that creating to push queue :
   * [SocketData(price=341.8169, time=2021-11-19T22:39:03.680425Z)]
   *
   * @param QuoteEvent coming from 3rd party application over socket it consist instrument name (isin) and its price.
   */
  quoteStream.connect { event ->
    QueueManager.pushMessage(event.data.isin, SocketData(event.data.price, Instant.now()))
  }
  server.start()
}

val jackson: ObjectMapper =
  jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

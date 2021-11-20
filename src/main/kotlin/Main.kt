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
  //val queueManager = QueueManager<Price>(30);
  //val queueManager: QueueManager<Candlestick> = QueueManager.getInstance(30)

  /**
   * InstrumentEvent(type=ADD, data=Instrument(isin=SJ3124P43460, description=commune fuisset ac tristique ex maiestatis))
   * It connects over instruments topics - shows added topics
   */
  instrumentStream.connect { event ->
    // TODO - implement
    if (event.type == InstrumentEvent.Type.ADD)
      QueueManager.createTopic(event.data.isin)
    if (event.type == InstrumentEvent.Type.DELETE)
      QueueManager.removeTopic(event.data.isin)
    println(QueueManager.getTopics())
    //println(event)
  }

  /**
   * QuoteEvent(data=Quote(isin=IA5E35112712, price=1073.3978))
   * It connects over quotes topics - shows added updated prices
   */
  quoteStream.connect { event ->
    // TODO - implement
    QueueManager.pushMessage(event.data.isin, SocketData(event.data.price, Instant.now()))
    //println(event)
  }

  server.start()
}

val jackson: ObjectMapper =
  jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

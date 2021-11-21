import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object QueueManager {
    private val msgQ = ConcurrentHashMap<String, ConcurrentLinkedQueue<SocketData>>()
    private const val DATA_KEEPING_MINUTE: Int = 30

    fun createTopic(topic: String) {
        msgQ[topic] = ConcurrentLinkedQueue()
    }

    fun removeTopic(topic: String?) {
        msgQ.remove(topic)
    }

    fun getTopics(): Set<String?> {
        return msgQ.keys
    }

    fun pushMessage(key: String, `object`: SocketData) {
        if (msgQ.containsKey(key)) {
            for ((_, time) in msgQ[key]!!) {
                val differences: Int =
                    Instant.now().atZone(ZoneOffset.UTC).minute - time.atZone(ZoneOffset.UTC).minute
                if (differences < DATA_KEEPING_MINUTE) {
                    break
                }
                msgQ[key]!!.remove()
            }
            msgQ[key]!!.add(`object`)
        }
    }

    fun getMessage(key: String): Queue<SocketData>? {
        return msgQ[key]
    }
}
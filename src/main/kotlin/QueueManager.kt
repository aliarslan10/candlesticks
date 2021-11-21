import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This QueueManager provide basic queue usage in the application.
 * It provides queue logic with ConcurrentHashMap. Key of the HashMap involves string parameter.
 * And value of the HashMap involves ConcurrentLinkedQueue. With this QueueManager, can be created
 * more than one queue with different topics. Each queue keeps their data only 30 minutes.
 */
object QueueManager {
    private val msgQ = ConcurrentHashMap<String, ConcurrentLinkedQueue<SocketData>>()
    private const val DATA_KEEPING_MINUTE: Int = 30

    /**
     * Creates a message topic like popular message brokers (kafka, rabbitmq etc.) with empty queue value.
     *
     * @param String name of the topic
     */
    fun createTopic(topic: String) {
        msgQ[topic] = ConcurrentLinkedQueue()
    }

    /**
     * Deletes message topic from the HashMap
     *
     * @param String name of the topic
     */
    fun removeTopic(topic: String?) {
        msgQ.remove(topic)
    }

    /**
     * Get all avaible topics from HashMap
     *
     * @return Set<String?> returns list of the topic names
     */
    fun getTopics(): Set<String?> {
        return msgQ.keys
    }

    /**
     * This function, data can be pushed to available topic's queue.
     * If there is data older than 30 minutes, it removes the data from the queue.
     * To avoid the iterate all the loop, if there is no data older than 30 minutes, loop is breaking.
     *
     * Example socket data [SocketData(price=341.8169, time=2021-11-19T22:39:03.680425Z)]
     *
     * @param String key is name of the topic
     * @param SocketData value of the topic and item of the queue
     */
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

    /**
     * Gets all message of specified topic
     *
     * @param String of the available topic into HashMap
     * @return Queue<SocketData> return list of the SocketData by Queue
     */
    fun getMessage(key: String): Queue<SocketData>? {
        return msgQ[key]
    }
}
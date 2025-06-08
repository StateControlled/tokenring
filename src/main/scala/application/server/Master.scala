package application.server

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.util.Timeout
import application.core.*
import com.typesafe.config.ConfigFactory
import sttp.client4.SttpClientException.TimeoutException

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @param events    a list of [[Event]]
 */
class Master(override val id : Int, events: List[Event]) extends Kiosk(id) {
    private val config                          = ConfigFactory.load()
    private val chunkSize                       = config.getInt("server.allocation.chunks-per-event")
    private val kioskName                       = config.getString("server.naming.node-actor-name")
    private var chunksToAllocate: List[Chunk]   = List.empty
    private var kiosks: List[ActorRef]          = List.empty

    implicit val timeout: Timeout = Timeout(2 seconds)

    buildSystem()

    private def buildSystem(): Unit = {
        populateChunks()
        initializeKiosks()
    }

    /**
     * Creates one chunk for each event with the maximum number of tickets available.
     * These chunks will be passed to each kiosk and each kiosk will take its allocation of tickets.
     */
    private def populateChunks(): Unit = {
        log.info("Populating Chunks...")
        events.foreach(event => {
            val chunk: Chunk = new Chunk(event, event.getCapacity, "A")
            chunksToAllocate = chunk :: chunksToAllocate
        })
    }

    /**
     * Creates the Kiosks that will be Nodes in the ring.
     */
    private def initializeKiosks(): Unit = {
        log.info("Creating Actors...")

        val kiosk4 = context.system.actorOf(Props(classOf[Kiosk], 4), name = s"${kioskName}4")
        val kiosk3 = context.system.actorOf(Props(classOf[Kiosk], 3), name = s"${kioskName}3")
        val kiosk2 = context.system.actorOf(Props(classOf[Kiosk], 2), name = s"${kioskName}2")
        val kiosk1 = context.system.actorOf(Props(classOf[Kiosk], 1), name = s"${kioskName}1")

        kiosks = kiosk4 :: kiosk3 :: kiosk2 :: kiosk1 :: kiosks

        nextNode = kiosk1
        kiosk1 ! SET_NEXT_NODE(kiosk2)
        kiosk2 ! SET_NEXT_NODE(kiosk3)
        kiosk3 ! SET_NEXT_NODE(kiosk4)
        kiosk4 ! SET_NEXT_NODE(context.self)

        kiosks.foreach(kiosk => kiosk ! SET_MASTER(context.self))

        distributeChunks()
    }

    private def distributeChunks(): Unit = {

        nextNode ! ALLOCATE_CHUNKS(chunksToAllocate, chunkSize)
    }

    override def receive: Receive = {
        case ALLOCATE_CHUNKS(chunks, chunkSize) =>
            receiveChunks(chunks)
        case SET_NEXT_NODE(node) =>
            nextNode = node
        case NEED_MORE_TICKETS(event, sendTo) =>
            // TODO
            handleNeedMoreTickets(event, sendTo)
        case STATUS_REPORT =>
            sender() !  STATUS_REPORT_ACK(handleStatusReport())
        case EVENTS_QUERY() =>
            handleEventsQuery()
        case LIST_CHUNKS =>
            handleListChunks()
        case STOP =>
            stop()
    }

    private def receiveChunks(chunks: List[Chunk]): Unit = {
        chunksToAllocate = chunks
        println("Chunks have been allocated.")
        println("Chunks remaining:")
        chunksToAllocate.foreach(chunk => {
            println(chunk)
        })
    }

    private def handleEventsQuery(): Unit = {
        println(s"Received query from ${sender()}. Sending event info")
        sender() ! EVENTS_QUERY_ACK(events)
    }

    private def handleStatusReport(): String = {
        println(s"${context.self.path}, Master $id collecting status reports")
        var listResult: List[String] = List.empty

        kiosks.foreach(kiosk =>
            try {
                val future: Future[Any] = kiosk ? STATUS_REPORT
                val result = Await.result(future, timeout.duration).asInstanceOf[STATUS_REPORT_ACK]
                println("Status Report:")
                println(result.response)
                listResult = result.response :: listResult
            } catch {
                case e: TimeoutException =>
                    println("Server timeout. Request failed.")
                    listResult = s"${kiosk.path.name} timeout" :: listResult
                case e: InterruptedException =>
                    println("Connection interrupted. Request failed.")
                    listResult = s"${kiosk.path.name} request interrupted" :: listResult
            }
        )

        if (listResult.isEmpty) {
            listResult = "[Master] Status report query failed" :: listResult
        }

        listResult.mkString(", ")
    }

    private def handleListChunks(): Unit = {
        chunksToAllocate.foreach(chunk => {
            println(chunk)
        })
    }

    private def handleNeedMoreTickets(title: String, sendTo: ActorRef): Unit = {
        // TODO
        val chunkResult: Option[Chunk] = chunksToAllocate.find(chunk => {
            chunk.getEventName.equalsIgnoreCase(title)
        })

        if (chunkResult.isDefined) {
            val chunk: Chunk = chunkResult.get
            val amount: Int = chunk.take(chunkSize)
            val chunkToSend = new Chunk(chunk.event,  amount, chunk.section)
            var list: List[Chunk] = List.empty
            list = chunkToSend :: list
            sendTo ! ALLOCATE_CHUNKS(list, amount)
        } else {
            // TODO request chunks from other kiosks if Master has no more
        }
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        // Supervisor strategy to allow the Master to restart crashed kiosks
        case _: RuntimeException => Restart
    }

    private def stop(): Unit = {
        context.system.terminate()
    }

}

package application.server

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import application.core.*
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @param events    a list of [[Event]]
 */
class Master(override val id : Int, events: List[Event]) extends Kiosk(id) {
    private val config                          = ConfigFactory.load()
    private val chunkSize                       = config.getInt("server.allocation.chunk-size")
    private val kioskName                       = config.getString("server.naming.node-actor-name")
    private var chunksToAllocate: List[Chunk]   = List.empty
    private var kiosks: List[ActorRef]          = List.empty
    private var salesRecords: List[SalesRecord] = List.empty
    private var soldOutEvents: mutable.Map[Event, Boolean] = mutable.Map.empty

    override def postStop(): Unit = {
        super.postStop()
    }

    initSystem()

    private def initSystem(): Unit = {
        populateChunks()
        initializeKiosks()
    }

    // check sales at interval
    context.system.scheduler.scheduleAtFixedRate(
        60.seconds,
        10.seconds,
        nextNode,
        SALES_REPORT(soldOutEvents)
    )

    /**
     * Creates one chunk for each event with the maximum number of tickets available.
     * These chunks will be passed to each kiosk and each kiosk will take its allocation of tickets.
     */
    private def populateChunks(): Unit = {
        log.info("[Master] Populating Chunks...")
        events.foreach(event => {
            val chunk: Chunk = new Chunk(event, event.getCapacity)
            chunk.setIsTotallySoldOut(false)
            chunksToAllocate = chunk :: chunksToAllocate    // create a chunk with all possible tickets for each event

            soldOutEvents += (event -> false) // mark all events as not sold out
        })
    }

    /**
     * Creates the Kiosks that will be Nodes in the ring.
     */
    private def initializeKiosks(): Unit = {
        log.info("[Master] Creating Actors...")

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

        distributeChunks(chunksToAllocate, chunkSize)
    }

    /**
     * Send chunks around the ring to distribute them.
     *
     * @param chunks    a list of chunks containing all events
     * @param size      the maximum number of tickets each kiosk may take
     */
    private def distributeChunks(chunks: List[Chunk], size: Int): Unit = {
        nextNode ! ALLOCATE_CHUNKS(chunks, size, -1)
    }

    override def receive: Receive = {
        case ALLOCATE_CHUNKS(chunks, chunkSize, destinationId) =>
            updateChunks(chunks, chunkSize)
        case EVENTS_QUERY() =>
            handleEventsQuery()
        case LIST_CHUNKS =>
            handleListChunks()
        case TICKET_ASK(event, recipientId) =>
            handleTicketAsk(event, recipientId)
        case SALES_REPORT(salesReport) =>
            handleSalesReport(salesReport)
        case STOP =>
            stop()
    }

    private def handleSalesReport(salesReport: mutable.Map[Event, Boolean]): Unit = {
        soldOutEvents = salesReport
    }

    private def handleTicketAsk(title: String, requesterId: ActorRef): Unit = {
        nextNode ! NEED_MORE_TICKETS(title, requesterId)
    }

    /**
     * Continuously pass chunks around.
     *
     * @param chunks        event chunks
     * @param chunkSize     the maximum number of tickets a kiosk can take
     */
    private def updateChunks(chunks: List[Chunk], chunkSize: Int): Unit = {
        chunksToAllocate = chunks
//        println("[Master] Chunks have been (re)allocated.")
//        println("[Master] Chunks remaining:")
//        chunksToAllocate.foreach(chunk => {
//            println(chunk)
//        })

        Thread.sleep(2000)

        distributeChunks(chunks, chunkSize)
    }

    private def handleEventsQuery(): Unit = {
        println(s"[Master] Received query from ${sender()}. Sending event info")
        sender() ! EVENTS_QUERY_ACK(events)
    }

    private def handleListChunks(): Unit = {
        println("[MASTER] Chunks in reserve:")
        chunksToAllocate.foreach(chunk => {
            println(chunk)
        })
        kiosks.foreach(kiosk => kiosk ! LIST_CHUNKS)
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        // Supervisor strategy to allow the Master to restart crashed kiosks
        case _: RuntimeException => Restart
    }

    private def stop(): Unit = {
        context.system.terminate()
    }

}

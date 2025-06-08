package application.server

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import application.core.*
import com.typesafe.config.ConfigFactory
import sttp.client4.SttpClientException.TimeoutException

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

    initSystem()

    private def initSystem(): Unit = {
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
            val chunk: Chunk = new Chunk(event, event.getCapacity)
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

        distributeChunks(chunksToAllocate, chunkSize)
    }

    /**
     * Send chunks around the ring to distribute them.
     *
     * @param chunks    a chunk containing all events
     * @param size      the number of tickets each kiosk may take
     */
    private def distributeChunks(chunks: List[Chunk], size: Int): Unit = {
        nextNode ! ALLOCATE_CHUNKS(chunks, size)
    }

    override def receive: Receive = {
        case ALLOCATE_CHUNKS(chunks, chunkSize) =>
            updateChunks(chunks)
        case NEED_MORE_TICKETS(event, sendTo) =>
            handleNeedMoreTickets(event, sendTo)
        case EVENTS_QUERY() =>
            handleEventsQuery()
        case LIST_CHUNKS =>
            handleListChunks()
        case STOP =>
            stop()
    }

    private def updateChunks(chunks: List[Chunk]): Unit = {
        chunksToAllocate = chunks
        println("Chunks have been allocated.")
        println("Chunks remaining:")
        chunksToAllocate.foreach(chunk => {
            println(chunk)
        })

//        nextNode ! ALLOCATE_CHUNKS(chunksToAllocate, 1, true)
    }

    private def handleEventsQuery(): Unit = {
        println(s"Received query from ${sender()}. Sending event info")
        sender() ! EVENTS_QUERY_ACK(events)
    }

    private def handleListChunks(): Unit = {
        chunksToAllocate.foreach(chunk => {
            println(chunk)
        })
    }

    /**
     * If a kiosk requests tickets, send tickets (if available)
     *
     * @param title
     * @param sendTo
     */
    private def handleNeedMoreTickets(title: String, sendTo: ActorRef): Unit = {
        val chunkResult: Option[Chunk] = chunksToAllocate.find(chunk => {
            chunk.getEventName.equalsIgnoreCase(title)
        })

        if (chunkResult.isDefined) {
            // TODO
            var chunkToSend: List[Chunk] = List.empty
            val chunk: Chunk = chunkResult.get
            chunkToSend = chunk :: chunkToSend
            val size: Int = chunk.getTicketsRemaining / kiosks.size
            // start ring
            nextNode ! ALLOCATE_CHUNKS(chunkToSend, size)
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

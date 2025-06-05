package application.server

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import application.core.*
import com.typesafe.config.ConfigFactory

/**
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @param events    a list of [[Event]]
 */
class Master(override val id : Int, events: List[Event]) extends Kiosk(id) {
    private val config                  = ConfigFactory.load()
//    private val numberOfKiosks = config.getInt("server.allocation.number-of-kiosks")
    private val numberOfChunksPerEvent  = config.getInt("server.allocation.chunks-per-event")
    private val kioskName               = config.getString("server.naming.node-actor-name")
    private var chunksToAllocate: List[List[Chunk]] = List.empty
    private var kiosks: List[ActorRef] = List.empty

    initializeKiosks()
    allocate()

    private def allocate(): Unit = {
        // split the total capacity (total number of tickets for an event) across the given number of chunks
        populateChunks()

        log.info("Allocating Chunks To Kiosks...")
        var i: Int = 0
        kiosks.foreach(kiosk => {
            log.info(s"Allocating chunks to ${kiosk.toString}")
            chunksToAllocate.foreach(list => {
                kiosk ! ALLOCATE_CHUNK(list(i))
                list(i).setIsAllocated(true)
            })
            i = i + 1
        })
    }

    /**
     * Divides the number of available tickets across several [[Chunk Chunks]]. Chunks will receive a set amount of tickets.
     * If the number cannot be evenly divided, the last Chunk will have only the remainder.
     */
    private def populateChunks(): Unit = {
        var section: String = "A"
        log.info("Populating Chunks...")
        events.foreach(event => {
            val initialChunkAllocation = event.getCapacity / numberOfChunksPerEvent
            val remainder = event.getCapacity - (initialChunkAllocation * numberOfChunksPerEvent)
            var chunkList: List[Chunk] = (1 to numberOfChunksPerEvent).toList.map(i => new Chunk(event, initialChunkAllocation, section))
            if (remainder > 0) {
                chunkList = new Chunk(event, remainder, section) :: chunkList
            }
            section = nextSection(section)
            chunksToAllocate = chunkList :: chunksToAllocate
        })

//        chunksToAllocate.foreach(chunkList => {
//            log.info(s"Chunks for ${chunkList.head.event.getName}")
//            chunkList.foreach(chunk => {
//                log.info(chunk.toString)
//            })
//        })
    }

    private def nextSection(section: String): String = {
        val c: Char = section.charAt(0)
        val d = c.+(1)
        s"${d.toChar}"
    }

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
    }

    override def receive: Receive = {
        case SET_NEXT_NODE(node) =>
            nextNode = node
        case token: TOKEN =>
            log.info(s"${context.self.path}, Master $id received token ${token.id}")
        case NEED_MORE_TICKETS(event: Event) =>
            sendChunk(event)
        case STATUS_REPORT() =>
            log.info(s"${context.self.path}, Master $id")
            kiosks.foreach(kiosk => kiosk ! STATUS_REPORT())
        case STATUS_REPORT_ACK(ack) =>
            println(ack)
        case EVENTS_QUERY() =>
            log.info(s"Received query from ${sender()}. Sending event info")
            sender() ! EVENTS_QUERY_ACK(events)
        case START(token: TOKEN) =>
            nextNode ! token
        case STOP =>
            context.system.terminate()
    }

    private def sendChunk(event: Event): Unit = {
        // TODO
        kiosks.foreach(kiosk => kiosk ! NEED_MORE_TICKETS(event))
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
        // Supervisor strategy to allow the Master to restart crashed kiosks
        case _: RuntimeException => Restart
    }

}

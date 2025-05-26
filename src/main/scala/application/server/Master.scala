package application.server

import akka.actor.{ActorRef, Props}
import application.core.{Chunk, Event, Venue}
import com.typesafe.config.ConfigFactory

/**
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @param venues    a list of [[Venue]]
 * @param events    a list of [[Event]]
 */
class Master(override val id : Int, venues: List[Venue], events: List[Event]) extends Kiosk(id) {
    private val config = ConfigFactory.load()
//    private val numberOfKiosks = config.getInt("server.allocation.number-of-kiosks")
    private val numberOfChunksPerEvent = config.getInt("server.allocation.chunks-per-event")
    private var chunksToAllocate: List[List[Chunk]] = List.empty

    private var kiosks: List[ActorRef] = List.empty

    initRing()
    allocate()

    private def allocate(): Unit = {
        // split the total capacity (total number of tickets for an event) across the given number of chunks
        populateChunks()

        log.info("Allocating Chunks To Kiosks...")
        var i: Int = 0
        kiosks.foreach(kiosk => {
            // TODO allocate chunks to Kiosks
            log.info(s"Allocating chunks to ${kiosk.toString}")
            chunksToAllocate.foreach(list => {
                kiosk ! AllocateChunk(list(i))
                list(i).setIsAllocated(true)
            })
            i = i + 1
        })
    }

    private def populateChunks(): Unit = {
        log.info("Populating Chunks...")
        events.foreach(event => {
            val initialChunkAllocation = event.getCapacity / numberOfChunksPerEvent
            val remainder = event.getCapacity - (initialChunkAllocation * numberOfChunksPerEvent)
            var chunkList: List[Chunk] = (1 to numberOfChunksPerEvent).toList.map(i => new Chunk(event, initialChunkAllocation))
            if (remainder > 0) {
                chunkList = new Chunk(event, remainder) :: chunkList
            }
            chunksToAllocate = chunkList :: chunksToAllocate
        })

//        chunksToAllocate.foreach(chunkList => {
//            log.info(s"Chunks for ${chunkList.head.event.getName}")
//            chunkList.foreach(chunk => {
//                log.info(chunk.toString)
//            })
//        })
    }

    private def initRing(): Unit = {
        log.info("Creating Actors...")
        val node04 = context.actorOf(Props(classOf[Kiosk], 4), name = "node4")
        val node03 = context.actorOf(Props(classOf[Kiosk], 3), name = "node3")
        val node02 = context.actorOf(Props(classOf[Kiosk], 2), name = "node2")
        val node01 = context.actorOf(Props(classOf[Kiosk], 1), name = "node1")

        kiosks = node04 :: node03 :: node02 :: node01 :: kiosks

        nextNode = node01
        node01 ! SetNextNode(node02)
        node02 ! SetNextNode(node03)
        node03 ! SetNextNode(node04)
        node04 ! SetNextNode(context.self)
    }

    override def receive: Receive = {
        case SetNextNode(node) =>
            nextNode = node
        case token: Token =>
            log.info(s"${context.self.path}, Master $id received token ${token.id}")
        case NeedMoreTickets(event: Event) =>
            sendChunk(event)
        case STATUS_REPORT =>
            log.info(s"${context.self.path}, Master $id")
            kiosks.foreach(kiosk => kiosk ! STATUS_REPORT)
        case Start(token: Token) =>
            nextNode ! token
        case Stop =>
            context.system.terminate()
    }

    private def sendChunk(event: Event): Unit = {
        // TODO
    }

}

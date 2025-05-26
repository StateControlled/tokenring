package application.server

import akka.actor.{ActorRef, Props}
import com.typesafe.config.ConfigFactory
import application.core.{Chunk, Event, Venue}

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
        events.foreach(event => {
            val initialChunkAllocation = event.getCapacity / numberOfChunksPerEvent
            val remainder = event.getCapacity - (initialChunkAllocation * numberOfChunksPerEvent)
            var chunkList: List[Chunk] = (1 to numberOfChunksPerEvent).toList.map(i => new Chunk(event, initialChunkAllocation))
            if (remainder > 0) {
                chunkList = new Chunk(event, remainder) :: chunkList
            }
            chunksToAllocate = chunkList :: chunksToAllocate
        })

        println("Allocated Chunks...")
        chunksToAllocate.foreach(cList => {
            cList.foreach(c => {
                println(c)
            })
        })

        println("Allocated Chunks To Kiosks...")
        kiosks.foreach(kiosk => {
            // TODO allocate chunks to Kiosks
            println(kiosk.toString)
        })

    }

    private def initRing(): Unit = {
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
            println(s"${context.self.path}, Master $id received token ${token.id}")
        case NeedMoreTickets(event: Event) =>
            sendChunk(event)
        case Start(token: Token) =>
            nextNode ! token
        case Stop =>
            context.system.terminate()
    }

    private def sendChunk(event: Event): Unit = {
        // TODO
    }

    override def toString: String = {
        s"MASTER-${context.self.path.name}-$id"
    }

}

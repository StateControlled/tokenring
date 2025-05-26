package application.server

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
    private val numberOfChunksPerEvent = config.getInt("server.allocation.chunks-per-event")
    private var chunksToAllocate: List[List[Chunk]] = List.empty
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

        chunksToAllocate.foreach(cList => {
            cList.foreach(c => {
                println(c)
            })
        })

        // TODO allocate chunks to Kiosks
    }

    override def receive: Receive = {
        case SetNextNode(node) =>
            nextNode = node
        case token: Token =>
            println(s"server.Master $id received token ${token.id}")
        case Start(token: Token) =>
            nextNode ! token
        case Stop =>
            context.system.terminate()
    }

}

package application.server

import akka.actor.{Actor, ActorRef}
import application.core.{Chunk, Event}
/**
 * A <code>Node</code> in the token ring system.
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @see [[Master]]
 */
class Kiosk(val id : Int) extends Actor {
    protected var nextNode: ActorRef = _
    private var eventTicketsOnSale: List[Chunk] = List.empty

    override def receive: Receive = {
        case SetNextNode(node) =>
            setNextNode(node)
        case SetChunk(chunk) =>
            eventTicketsOnSale = chunk :: eventTicketsOnSale
        case token: Token =>
            println(s"server.Kiosk $id received token ${token.id}")
            process()
            nextNode ! token
        case Buy(amount: Int, event: Event) =>
            // TODO buy tickets
        case Stop =>
            // TODO stop logic
    }

    // TODO
    private def process(): Unit = {
        Thread.sleep(500) // Simulate processing time for now
    }

    /**
     * @return  the [[ActorRef]] for this [[Kiosk]] neighbor node in the token-ring system
     */
    def getNextNode: ActorRef = {
        nextNode
    }

    private def setNextNode(next: ActorRef): Unit = {
        nextNode = next
    }

    def getEventsOnSale: List[Chunk] = {
        eventTicketsOnSale
    }

}

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
            println(s"${context.self.path}, Kiosk $id received token ${token.id}")
            process()
            nextNode ! token
        case Buy(amount: Int, event: Event) =>
            // TODO buy tickets
            buy()
            eventTicketsOnSale.foreach(chunk => {
                if (!check(chunk)) {
                    context.parent ! NeedMoreTickets(chunk.event)
                }
            })
        case Stop =>
            // TODO stop logic
    }

    private def buy(): Unit = {
        // TODO
    }

    private def check(chunk: Chunk): Boolean = {
        chunk.ticketsRemaining > 0
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

    override def toString: String = {
        s"TK${context.self.path.name}-$id"
    }

}

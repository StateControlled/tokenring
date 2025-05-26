package application.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import application.core.{Chunk, Event}
/**
 * A <code>Node</code> in the token ring system.
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @see [[Master]]
 */
class Kiosk(val id : Int) extends Actor with ActorLogging {
    protected var nextNode: ActorRef = _
    private var eventTicketsOnSale: List[Chunk] = List.empty

    override def receive: Receive = {
        case SetNextNode(node) =>
            setNextNode(node)
        case AllocateChunk(chunk) =>
            eventTicketsOnSale = chunk :: eventTicketsOnSale
        case STATUS_REPORT =>
            eventTicketsOnSale.foreach(chunk => {
                log.info(s"Kiosk ${context.self.path.name}; Tickets on sale: ${chunk.toString}, ${chunk.ticketsRemaining} tickets remaining.")
            })
        case token: Token =>
            log.info(s"${context.self.path}, Kiosk $id received token ${token.id}")
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

    private def buy(): Boolean = {
        // TODO
        false
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
    private def getNextNode: ActorRef = {
        nextNode
    }

    /**
     * Sets this [[Kiosk]] neighbor, the next node in a token-ring system.
     *
     * @param next  the node to become the neighbor
     */
    private def setNextNode(next: ActorRef): Unit = {
        nextNode = next
    }

    private def getEventsOnSale: List[Chunk] = {
        eventTicketsOnSale
    }

}

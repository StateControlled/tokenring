package application.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import application.core.*
/**
 * A <code>Node</code> in the token ring system.
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @see [[Master]]
 */
class Kiosk(val id : Int) extends Actor with ActorLogging {
    protected var nextNode: ActorRef = _
    protected var master: ActorRef = _
    private var eventTicketsOnSale: List[Chunk] = List.empty
    
    override def receive: Receive = {
        case SET_NEXT_NODE(node) =>
            setNextNode(node)
        case ALLOCATE_CHUNK(chunk) =>
            eventTicketsOnSale = chunk :: eventTicketsOnSale
        case STATUS_REPORT() =>
            eventTicketsOnSale.foreach(chunk => {
                log.info(s"STATUS Kiosk ${context.self.path.name}; Tickets on sale: ${chunk.toString}, ${chunk.getTicketsRemaining} tickets remaining.")
            })
            sender() ! STATUS_REPORT_ACK(s"STATUS Kiosk ${context.self.path.name} is online.")
        case token: TOKEN =>
            log.info(s"${context.self.path}, Kiosk $id received token ${token.id}")
            process()
            nextNode ! token
        case NEED_MORE_TICKETS(event: Event) =>
            // TODO
            val e: Option[Chunk] = eventExists(event.getName)
            if (check(e.get)) {

            }
        case BUY(amount: Int, title: String) =>
            // TODO buy tickets
            val e: Option[Chunk] = eventExists(title)
            if (e.isDefined) {
                // if there is a chunk of tickets for the requested event
                var ticketOrder: List[Ticket] = tryBuy(amount, e.get)
                if (ticketOrder.length == amount) {
                    // order succeeds with complete number of tickets
                    afterBuy()
                    sender() ! ORDER(ticketOrder)
                } else {
                    // TODO query other kiosks for tickets
                    // if there are none, send failure message
                    master ! NEED_MORE_TICKETS(e.get.getEvent)
                }
            } else {
                // no such option exists, no event
                sender() ! EVENT_DOES_NOT_EXIST(title)
            }
        case message: String =>
            println(s"${context.self.path.name} received (string) message: $message")
        case STOP =>
            // TODO stop logic
    }

    private def tryBuy(amount: Int, chunk: Chunk): List[Ticket] = {
        val sold = chunk.take(amount)
        if (sold == amount) {
            return List.fill(amount)(new Ticket(chunk.getVenue.getName, chunk.getEvent.getName, chunk.getEvent.getDate))
        } else {
            return List.fill(sold)(new Ticket(chunk.getVenue.getName, chunk.getEvent.getName, chunk.getEvent.getDate))
        }
    }

    private def afterBuy(): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            if (!check(chunk)) {
                context.parent ! NEED_MORE_TICKETS(chunk.event)
            }
        })
    }

    /**
     * Check that an event exists. Does not check if there are tickets available.
     *
     * @param title event title
     * @return  an [[Option]] with the corresponding [[Chunk]] if it exists.
     */
    private def eventExists(title: String): Option[Chunk] = {
        eventTicketsOnSale.find(chunk => chunk.getEventName.equalsIgnoreCase(title))
    }

    private def check(chunk: Chunk): Boolean = {
        chunk.getTicketsRemaining > 0
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

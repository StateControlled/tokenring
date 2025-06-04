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
        case SET_MASTER(master) =>
            setMaster(master)
        case ALLOCATE_CHUNK(chunk) =>
            handleAllocateChunk(chunk)
        case STATUS_REPORT() =>
            handleStatusReport()
        case token: TOKEN =>
            handleToken(token)
        case NEED_MORE_TICKETS(event: Event) =>
            // TODO
            val e: Option[Chunk] = eventExists(event.name)
            if (check(e.get)) {

            }
        case BUY(amount: Int, title: String) =>
            // TODO buy tickets
            handleBuy(amount, title)
        case message: String =>
            println(s"${context.self.path.name} received (string) message: $message")
    }

    private def handleBuy(amount: Int, title: String): Unit = {
        val e: Option[Chunk] = eventExists(title)
        if (e.isDefined) {
            // if there is a chunk of tickets for the requested event
            var ticketOrder: List[Ticket] = tryTakeTickets(amount, e.get)
            if (ticketOrder.length == amount) {
                // order succeeds with complete number of tickets
                //                    afterBuy()
                sender() ! ORDER(ticketOrder)
            } else {
                // TODO query other kiosks for tickets
                // if there are none, send failure message
                master ! NEED_MORE_TICKETS(e.get.event)
            }
        } else {
            // no such option exists, no event
            sender() ! EVENT_DOES_NOT_EXIST(title)
        }
    }

    private def tryTakeTickets(amount: Int, chunk: Chunk): List[Ticket] = {
        val sold = chunk.take(amount)
        if (sold == amount) {
            println(s"Sold $sold tickets for ${chunk.getEventName}")
            return makeTicketOrder(amount, chunk)
        } else {
            println(s"Sold $sold tickets for ${chunk.getEventName}")
            return makeTicketOrder(amount, chunk)
        }
    }

    private def makeTicketOrder(amount: Int, chunk: Chunk): List[Ticket] = {
        var result: List[Ticket] = List.empty

        for (t <- 1 to amount)
            result = Ticket(chunk.getVenueName, chunk.getEventName, chunk.getEventDate, s"${chunk.section}$t") :: result

        result
    }

    /**
     * Runs after a successful ticket purchase
     */
    private def afterBuy(): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            if (!check(chunk)) {
                context.parent ! NEED_MORE_TICKETS(chunk.event)
            }
        })
    }

    /**
     * Handles [[STATUS_REPORT]] messages from the Master actor
     */
    private def handleStatusReport(): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            log.info(s"STATUS Kiosk ${context.self.path.name}; Tickets on sale: ${chunk.toString}, ${chunk.getTicketsRemaining} tickets remaining.")
        })
        sender() ! STATUS_REPORT_ACK(s"STATUS Kiosk ${context.self.path.name} is online.")
    }

    private def handleAllocateChunk(chunk: Chunk): Unit = {
        eventTicketsOnSale = chunk :: eventTicketsOnSale
    }

    /**
     * @param chunk the [[Chunk]] to check
     * @return  <code>true</code> if the [[Chunk]] has tickets remaining.
     */
    private def check(chunk: Chunk): Boolean = {
        chunk.getTicketsRemaining > 0
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

    /**
     * Handles receiving a [[TOKEN]]
     *
     * @param token the [[TOKEN]]
     */
    // TODO
    private def handleToken(token: TOKEN): Unit = {
        log.info(s"${context.self.path}, Kiosk $id received token ${token.id}")
        Thread.sleep(500) // Simulate processing time for now
        nextNode ! token
    }

    /**
     * Sets this [[Kiosk]] neighbor, the next node in a token-ring system.
     *
     * @param next  the node to become the neighbor
     */
    private def setNextNode(next: ActorRef): Unit = {
        nextNode = next
    }

    /**
     * Sets a reference to the [[Master]] actor in the system.
     *
     * @param masterActor   an ActorRef to the Master actor
     */
    private def setMaster(masterActor: ActorRef): Unit = {
        master = masterActor
    }

    /**
     * @return a list of the Events that have chunks allocated to this Kiosk
     */
    private def getEventsOnSale: List[Chunk] = {
        eventTicketsOnSale
    }

}

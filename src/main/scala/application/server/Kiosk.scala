package application.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
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
    private val cluster: Cluster = Cluster(context.system)

    override def preStart(): Unit = {
        // subscribe to cluster changes, re-subscribe when restart
        println(s"${self.toString} Starting...")
        cluster.subscribe(
            self,
            initialStateMode=InitialStateAsEvents,
            classOf[MemberEvent],
            classOf[UnreachableMember]
        )
    }

    override def postStop(): Unit = {
        println(s"${self.toString} Stopping...")
        cluster.unsubscribe(self)
    }
    
    override def receive: Receive = {
        case state: CurrentClusterState =>
            log.info("Current members: {}", state.members.mkString(", "))
        case MemberUp(member) =>
            log.info(s"Member is Up: ${member.address}")
        case UnreachableMember(member) =>
            log.info(s"Member detected as unreachable: $member")
        case MemberRemoved(member, previousStatus) =>
            log.info(s"Member is Removed: ${member.address} after $previousStatus")
        case _: MemberEvent =>
            log.info("Unhandled Cluster Member Event")
        case SET_NEXT_NODE(node) =>
            setNextNode(node)
        case SET_MASTER(master) =>
            setMaster(master)
        case ALLOCATE_CHUNK(chunk) =>
            handleAllocateChunk(chunk)
        case STATUS_REPORT =>
            handleStatusReport()
        case token: TOKEN =>
            handleToken(token)
        case NEED_MORE_TICKETS(event: Event) =>
            // TODO
            val e: Option[Chunk] = eventExists(event.name)
            if (check(e.get)) {

            }
        case BUY(title: String) =>
            handleBuy(title)
        case SELF_DESTRUCT =>
            throw new RuntimeException("Self destruct order received. Goodbye.")
        case message: String =>
            handleStringMessage(message)
    }

    private def transferTicketsTo(requester: ActorRef, event: String, amount: Int): Unit = {

    }

    /////////////////////////////////

    /**
     * Purchase logic.
     *
     * @param title     the event title
     */
    private def handleBuy(title: String): Unit = {
        val event: Option[Chunk] = eventExists(title)
        if (event.isDefined) {
            // if there is a chunk of tickets for the requested event
            val ticketOrder: Option[Ticket] = tryTakeTickets(event.get)
            if (ticketOrder.isDefined) {
                // order succeeds with complete number of tickets
                sender() ! ORDER(ticketOrder.get)
            } else {
                // TODO query other kiosks for tickets
                // if there are none, send failure message
                master ! NEED_MORE_TICKETS(event.get.event)
            }
        } else {
            // no such option exists, no event
            sender() ! EVENT_DOES_NOT_EXIST(title)
        }
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
     * Attempts to take the given amount of [[Ticket tickets]] from the [[Chunk chunk]]. If not enough tickets remain,
     * takes the maximum amount possible.
     *
     * @param chunk     the chunk to take tickets from
     * @return          a list of Tickets
     */
    private def tryTakeTickets(chunk: Chunk): Option[Ticket] = {
        if (chunk.take()) {
            println(s"Sold a ticket for ${chunk.getEventName}")
            Some(Ticket(chunk.getVenueName, chunk.getEventName, chunk.getEventDate, s"${chunk.section}${chunk.getTicketsSold}"))
        } else {
            println(s"No tickets remaining for ${chunk.getEventName}")
            None
        }

    }

    /////////////////////////////////

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
            log.info(s"STATUS ${context.self.path.name}; Tickets on sale: ${chunk.toString}, ${chunk.getTicketsRemaining} tickets remaining.")
        })
        sender() ! STATUS_REPORT_ACK(s"STATUS Kiosk ${context.self.path.name} online")
    }

    /**
     * @param chunk the [[Chunk]] to check
     * @return  <code>true</code> if the [[Chunk]] has tickets remaining.
     */
    private def check(chunk: Chunk): Boolean = {
        chunk.getTicketsRemaining > 0
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

    private def handleStringMessage(message: String): Unit = {
        println(s"${context.self.path.name} received (string) message: $message")
    }

    private def handleAllocateChunk(chunk: Chunk): Unit = {
        eventTicketsOnSale = chunk :: eventTicketsOnSale
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

}

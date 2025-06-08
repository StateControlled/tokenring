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
    protected var nextNode: ActorRef = _ // null
    protected var master: ActorRef = _ // null
    private var eventTicketsOnSale: List[Chunk] = List.empty
    private val cluster: Cluster = Cluster(context.system)

    override def preStart(): Unit = {
        // called on instantiation
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
        // called when stopped
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
        case ALLOCATE_CHUNKS(chunk, size) =>
            handleAllocateChunk(chunk, size)
        case STATUS_REPORT =>
            handleStatusReport()
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
    // Purchase Logic

    /**
     * Handles a request to [[Buy purchase]] tickets. If a ticket is available for the given event, returns a [[Ticket]],
     * else requests more tickets from the [[Master]]
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
                master ! NEED_MORE_TICKETS(title, self)
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
        if (chunk.sellOne()) {
            println(s"Sold a ticket for ${chunk.getEventName}")
            Some(Ticket(chunk.getVenueName, chunk.getEventName, chunk.getEventDate, s"${chunk.section}${chunk.getTicketsSold}"))
        } else {
            println(s"No tickets remaining for ${chunk.getEventName}")
            None
        }

    }

    /////////////////////////////////

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

    private def handleStringMessage(message: String): Unit = {
        println(s"${context.self.path.name} received (string) message: $message")
    }

    /**
     * Take a portion of each chunk.
     *
     * @param chunks    a list of chunks
     */
    private def handleAllocateChunk(chunks: List[Chunk], chunkSize: Int): Unit = {
        chunks.foreach(chunk => {
            val allocation: Chunk = new Chunk(chunk.event, chunk.take(chunkSize), chunk.section)
            eventTicketsOnSale = allocation :: eventTicketsOnSale
            chunk.setSection(nextSection(chunk.section))
        })
        nextNode ! ALLOCATE_CHUNKS(chunks, chunkSize)
    }

    /**
     * Advances the first Char to the next char is ASCII ordering.
     *
     * @param section   a string
     * @return          the next in the series
     */
    private def nextSection(section: String): String = {
        val c: Char = section.charAt(0)
        val d = c.+(1)
        s"${d.toChar}"
    }

    //////////////////////////////////////////
    // Getters and Setters

    /**
     * Sets this [[Kiosk]] neighbor, the next node in a token-ring system.
     *
     * @param next  the node to become the neighbor
     */
    protected def setNextNode(next: ActorRef): Unit = {
        nextNode = next
    }

    /**
     * Sets a reference to the [[Master]] actor in the system.
     *
     * @param masterActor   an ActorRef to the Master actor
     */
    protected def setMaster(masterActor: ActorRef): Unit = {
        master = masterActor
    }

}

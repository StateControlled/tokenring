package application.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.*
import akka.util.Timeout
import application.core.*

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

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

    implicit val timeout: Timeout = Timeout(5 seconds)

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

        case LIST_CHUNKS =>
            handleListChunks()
        case SET_NEXT_NODE(node) =>
            setNextNode(node)
        case SET_MASTER(master) =>
            setMaster(master)
        case NEED_MORE_TICKETS(event, replyTo) =>
            handleNeedMoreTickets(event, replyTo)
        case SALES_REPORT(events) =>
            handleSalesReport(events)
        case EVENT_SOLD_OUT(event, tryAgain) =>
            handleSoldOut(event, tryAgain)
        case ALLOCATE_CHUNKS(chunk, size, destinationId) =>
            handleAllocateChunk(chunk, size, destinationId)
        case BUY(title: String) =>
            handleBuy(title)
            afterBuyCheck()
    }

    private def handleSoldOut(event: Event, tryAgain: Boolean): Unit = {
        val ch: Option[Chunk] = eventTicketsOnSale.find(chunk => chunk.event == event)
        if (ch.isDefined) {
            ch.get.setIsTotallySoldOut(true)
        }
    }

    private def handleSalesReport(events: mutable.Map[Event, Boolean]): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            val b: Option[Boolean] = events.get(chunk.event)
            if (b.isDefined) {
                // update map
                val soldOut: Boolean = b.get // event is sold out?
                events += (chunk.event -> (soldOut || chunk.isDepleted))
            }
        })
        // pass to next node in ring
        nextNode ! SALES_REPORT(events)
    }

    private def afterBuyCheck(): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            if (chunk.isDepleted) {
                master ! TICKET_ASK(chunk.event, self)
            }
        })
    }

    /**
     * If the Master sends a [[NEED_MORE_TICKETS]] message around the ring, this handles the response. If this Kiosk does
     * not have tickets for the event or does not have enough to split its remaining tickets, it passes the message to the next node in the ring.
     * Else it sends a chunk of tickets directly to the requester and stops the message from passing around the ring.
     *
     * @param event     the event
     * @param replyTo   the Kiosk that made the request
     */
    private def handleNeedMoreTickets(event: Event, replyTo: ActorRef): Unit = {
        if (self != replyTo) {
            val chunk: Option[Chunk] = eventTicketsOnSale.find(chunk => chunk.event == event)

            if (chunk.isDefined) {
                // send tickets if available
                val c: Chunk = chunk.get
                if (c.getTicketsRemaining > 1) {
                    val part: Int = c.take(c.getTicketsRemaining / 2) // take half
                    val replyChunk: Chunk = new Chunk(c.event, part)
                    replyTo ! TICKET_ASK_REPLY(replyChunk)
                    println(s"$self sent a chunk to $replyTo")
                    return
                }
            } else {
                nextNode ! NEED_MORE_TICKETS(event, replyTo)
            }
        } else {
            nextNode ! NEED_MORE_TICKETS(event, replyTo)
        }
    }

    /////////////////////////////////

    // Purchase Logic

    /**
     * Handles a request to [[Buy purchase]] tickets. If a ticket is available for the given event, returns a [[Ticket]],
     * else requests more tickets from the [[Master]] <br>
     * If the event does not exist, tell the Client so.
     *
     * @param title     the event title
     */
    private def handleBuy(title: String): Unit = {
        val chunk: Option[Chunk] = chunkExists(title)

        if (chunk.isDefined) {
            // if there is a chunk of tickets for the requested event
            val c: Chunk = chunk.get
            val ticketOrder: Option[Ticket] = tryTakeTickets(c)

            if (ticketOrder.isDefined) {
                // order succeeds with complete number of tickets
                val ticket: Ticket = ticketOrder.get
                sender() ! ORDER(ticket)
            } else {
                sender() ! EVENT_SOLD_OUT(c.event, chunk.get.isTotallySoldOut)
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
    private def chunkExists(title: String): Option[Chunk] = {
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
            println(s"Kiosk $id sold 1 ticket for ${chunk.getEventName}")
            Some(Ticket(chunk.getVenueName, chunk.getEventName, chunk.getEventDate))
        } else {
            println(s"[Kiosk $id] No tickets remaining for ${chunk.getEventName}")
            None
        }
    }

    /////////////////////////////////

    /**
     * Take a portion of each chunk.
     *
     * @param chunks    a list of chunks
     */
    private def handleAllocateChunk(chunks: List[Chunk], chunkSize: Int, destinationId: Int): Unit = {
        // Add more tickets
        chunks.foreach(chunk => {
            // find matching chunk
            val local: Option[Chunk] = eventTicketsOnSale.find(c => {
                c.getEventName.equalsIgnoreCase(chunk.getEventName)
            })
            // add inventory
            if (local.isDefined) {
                // to existing
                val localChunk: Chunk = local.get
                if (localChunk.isDepleted) {
                    localChunk.add(chunk.take(chunkSize))
                }
            } else {
                // new allocation
                val num: Int = chunk.take(chunkSize)
                val allocation: Chunk = new Chunk(chunk.event, num)
                eventTicketsOnSale = allocation :: eventTicketsOnSale
                println(s"Kiosk $id added a new chunk of ${allocation.getTicketsRemaining} tickets for ${allocation.getEventName}")
            }
        })
        nextNode ! ALLOCATE_CHUNKS(chunks, chunkSize, destinationId)
    }

    /**
     * Lists information about chunks
     */
    private def handleListChunks(): Unit = {
        val builder: StringBuilder  = new StringBuilder()
        builder.append(f"Kiosk $id events on sale:%n")
        eventTicketsOnSale.foreach(chunk => {
            builder.append(f"${chunk.toString}%n")
        })
        sender() ! GENERIC_RESPONSE(builder.toString)
    }

    //////////////////////////////////////////

    // Getters and Setters

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

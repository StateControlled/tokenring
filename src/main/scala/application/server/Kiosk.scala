package application.server

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Address, ReceiveTimeout}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import application.core.*
import akka.pattern.ask
import akka.util.Timeout
import sttp.client4.SttpClientException.TimeoutException
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
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

        case SET_NEXT_NODE(node) =>
            setNextNode(node)
        case SET_MASTER(master) =>
            setMaster(master)

        case ALLOCATE_CHUNKS(chunk, size, destinationId) =>
            handleAllocateChunk(chunk, size, destinationId)

        case BUY(title: String) =>
            handleBuy(title)
            afterBuyCheck()
        case SELF_DESTRUCT =>
            throw new RuntimeException("Self destruct order received. Goodbye.")
    }

    private def afterBuyCheck(): Unit = {
        eventTicketsOnSale.foreach(chunk => {
            if (chunk.isDepleted) {
                master ! NEED_MORE_TICKETS(chunk.getEventName, id)
            }
        })
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
        val chunk: Option[Chunk] = chunkExists(title)

        if (chunk.isDefined) {
            // if there is a chunk of tickets for the requested event
            val ticketOrder: Option[Ticket] = tryTakeTickets(chunk.get)
            if (ticketOrder.isDefined) {
                // order succeeds with complete number of tickets
                val ticket: Ticket = ticketOrder.get
                sender() ! ORDER(ticket)
            } else {
                sender() ! EVENT_SOLD_OUT(title, chunk.get.isTotallySoldOut)
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
            println(s"Sold a ticket for ${chunk.getEventName}")
            Some(Ticket(chunk.getVenueName, chunk.getEventName, chunk.getEventDate))
        } else {
            println(s"No tickets remaining for ${chunk.getEventName}")
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
            val local: Option[Chunk] = eventTicketsOnSale.find(chunk => {
                chunk.getEventName.equalsIgnoreCase(chunks.head.getEventName)
            })
            // add inventory
            if (local.isDefined) {
                // to existing
                val localChunk: Chunk = local.get
                if (localChunk.isDepleted) {
                    localChunk.add(chunk.take(chunkSize))
                }
                if (chunk.isDepleted) {
                    localChunk.setIsTotallySoldOut(true)
                }
            } else {
                // new allocation
                val allocation: Chunk = new Chunk(chunk.event, chunk.take(chunkSize))
                eventTicketsOnSale = allocation :: eventTicketsOnSale
            }
        })
        nextNode ! ALLOCATE_CHUNKS(chunks, chunkSize, destinationId)
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

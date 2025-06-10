package application.client

import akka.actor.{Actor, ActorSelection, Address}
import akka.pattern.ask
import akka.util.Timeout
import application.core.*
import com.typesafe.config.{Config, ConfigFactory}
import sttp.client4.SttpClientException.TimeoutException

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
 * Client Actor communicates with the Actor System to purchase tickets. It responds to commands from the [[ClientMain]] object
 */
class Client extends Actor {
    private var orders: List[Ticket] = List.empty

    private val config: Config          = ConfigFactory.load.getConfig("server")
    private val numberOfKiosks: Int     = ConfigFactory.load.getInt("server.allocation.number-of-kiosks")
    private val remoteAddress: Address  = setRemoteAddress()
    private var kioskId = 1

    private val masterName  = config.getString("naming.master-actor-name")
    private val kioskName   = config.getString("naming.node-actor-name")

    var kiosk: ActorSelection   = context.actorSelection(s"$remoteAddress/user/$kioskName$kioskId")
    var master: ActorSelection  = context.actorSelection(s"$remoteAddress/user/$masterName")

    // time to wait for a reply
    implicit val timeout: Timeout = Timeout(5 seconds)

    private def setRemoteAddress(): Address = {
        val hostname = ConfigFactory.load.getString("server.akka.remote.artery.canonical.hostname")
        val port = ConfigFactory.load.getInt("server.akka.remote.artery.canonical.port")

        val remoteAddress: Address = Address("akka", "TicketSelling", hostname, port)
        remoteAddress
    }

    override def receive: Receive = {
        case BUY(title: String) =>
            handleBuy(title)
        case EVENT_DOES_NOT_EXIST(title: String) =>
            handleNoEvent(title)
        case EVENTS_QUERY() =>
            handleEventsQuery()
        case EVENTS_QUERY_ACK(eventsList) =>
            handleEventQueryAck(eventsList)
        case SWITCH =>
            handleSelectKiosk()
        case ORDER(ticket: Ticket) =>
            handleOrderReceived(ticket)
        case PRINT_ORDERS =>
            printOrders()
        case SAVE_ORDERS =>
            saveOrders()
    }

    private def handleSoldOut(title: String): Unit = {

    }

    private def handleNoEvent(title: String): Unit = {
        println(s"The event $title does not exist.")
        println("Order could not be completed.")
    }

    /**
     * Sends a [[BUY]] message to a Kiosk and waits for result.
     *
     * @param title             the event title
     */
    private def handleBuy(title: String): Unit = {
        var orderResult: Ticket = null
        try {
            val future: Future[Any] = kiosk ? BUY(title)
            val result = Await.result(future, timeout.duration)
            result match
                case ORDER(ticket) =>
                    println("Tickets purchased:")
                    println(ticket)
                    orderResult = ticket
                case EVENT_DOES_NOT_EXIST(title) =>
                    handleNoEvent(title)
                case EVENT_SOLD_OUT(title, tryAgain) =>
                    if (tryAgain) {
                        handleSelectKiosk()
                        println("Order did not process, please try again.")
                    } else {
                        println(s"Sorry. The event ${title.name} is sold out.")
                    }
        } catch {
            case e: TimeoutException =>
                println("[CLIENT] Server timeout. Request failed.")
                handleSelectKiosk()
            case e: InterruptedException =>
                println("[CLIENT] Connection interrupted. Request failed.")
            case e: ClassCastException =>
                println("[CLIENT] Unhandled message return type")
        }

        if (orderResult != null) {
            orders = orderResult :: orders
        }
    }

    private def handleEventQueryAck(eventsList: List[Event]): Unit = {
        println("Events on sale:")
        printList(eventsList)
    }

    private def handleOrderReceived(ticket: Ticket): Unit = {
        println("Purchase successful!")
        orders = ticket :: orders
        println(ticket)
    }

    private def printOrders(): Unit = {
        if (orders.isEmpty) {
            println("No orders")
        } else {
            println("Orders:")
            orders.foreach(order => println(order))
        }
    }

    private def saveOrders(): Unit = {
        println("Saving orders...")
        val json: String = upickle.default.write(orders)
        os.write(os.pwd / "orders.json", json)
    }

    private def handleEventsQuery(): Unit = {
        master ! EVENTS_QUERY()
    }

    private def handleSelectKiosk(): Unit = {
        kioskId = kioskId + 1
        if (kioskId > numberOfKiosks) {
            kioskId = 1
        }
        kiosk = context.actorSelection(s"$remoteAddress/user/kiosk$kioskId")
    }

    private def printList(list: List[Any]): Unit = {
        list.foreach(e => println(e.toString))
    }

}

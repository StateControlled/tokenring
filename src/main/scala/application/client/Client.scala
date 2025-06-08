package application.client

import akka.actor.{Actor, ActorSelection, Address, ReceiveTimeout}
import akka.pattern.ask
import akka.util.Timeout
import application.core.*
import com.typesafe.config.{Config, ConfigFactory}
import sttp.client4.SttpClientException.TimeoutException

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
//import scala.util.{Failure, Success}

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
        case ReceiveTimeout =>
            handleTimeout()
        case SELF_DESTRUCT =>
            handleKillOrder()
        case PRINT_ORDERS =>
            printOrders()
        case SAVE_ORDERS =>
            saveOrders()
    }

    private def handleTimeout(): Unit = {
        println("TIMEOUT: No Response")
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
        var listResult: Ticket = null
        try {
            val future: Future[Any] = kiosk ? BUY(title)
            val result = Await.result(future, timeout.duration)
            result match
                case ORDER(ord) =>
                    println("Tickets purchased:")
                    println(ord)
                    listResult = ord
                case EVENT_DOES_NOT_EXIST(t) =>
                    handleNoEvent(t)
                case EVENT_SOLD_OUT(t) =>
                    println("Event sold out!")
                case _ =>
                    println("[CLIENT] Unhandled message return type")
        } catch {
            case e: TimeoutException =>
                println("[CLIENT] Server timeout. Request failed.")
                handleSelectKiosk()
            case e: InterruptedException =>
                println("[CLIENT] Connection interrupted. Request failed.")
                handleSelectKiosk()
            case e: ClassCastException =>
                println("[CLIENT] Unhandled message return type")
        }
        orders = listResult :: orders
    }

    private def handleEventQueryAck(eventsList: List[Event]): Unit = {
        println("Events on sale:")
        printList(eventsList)
    }

    private def handleStringMessage(msg: String): Unit = {
        println("Forwarding message to kiosk...")
        println("Message: " + msg)
        kiosk ! msg
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

    /**
     * Failure here may mean the entire system is down.
     * @see <a href="https://alvinalexander.com/scala/akka-actor-how-to-send-message-wait-for-reply-ask/">Wait for Reply</a>
     */
//    private def handleStatusReport(): Unit = {
//        try {
//            val future: Future[Any] = master ? STATUS_REPORT
//            val result = Await.result(future, timeout.duration).asInstanceOf[STATUS_REPORT_ACK]
//            println("Status Report:")
//            println(result.response)
//        } catch {
//            case e: TimeoutException =>
//                println("Server timeout. Request failed.")
//            case e: InterruptedException =>
//                println("Connection interrupted. Request failed.")
//        }
//    }

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

    private def handleKillOrder(): Unit = {
        kiosk ! SELF_DESTRUCT
    }

    private def printList(list: List[Any]): Unit = {
        list.foreach(e => println(e.toString))
    }

}

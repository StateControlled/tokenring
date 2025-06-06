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
    private var orders: List[List[Ticket]] = List.empty

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
        case BUY(ticketQuantity: Int, title: String) =>
            handleBuy(ticketQuantity, title)
        case EVENT_DOES_NOT_EXIST(title: String) =>
            handleNoEvent(title)
        case STATUS_REPORT =>
            handleStatusReport()
        case EVENTS_QUERY() =>
            handleEventsQuery()
        case EVENTS_QUERY_ACK(eventsList) =>
            handleEventQueryAck(eventsList)
        case SWITCH =>
            handleSelectKiosk()
        case ORDER(tickets: List[Ticket]) =>
            handleOrderReceived(tickets)
        case ReceiveTimeout =>
            handleTimeout()
        case SELF_DESTRUCT =>
            handleKillOrder()
        case PRINT_ORDERS =>
            printOrders()
        case SAVE_ORDERS =>
            saveOrders()
        case msg: String =>
            handleStringMessage(msg)
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
     * @param ticketQuantity    the number of tickets to buy
     * @param title             the event title
     */
    private def handleBuy(ticketQuantity: Int, title: String): Unit = {
        var listResult: List[Ticket] = List.empty
        try {
            val future: Future[Any] = kiosk ? BUY(ticketQuantity, title)
            val result = Await.result(future, timeout.duration).asInstanceOf[List[Ticket]]
            println("Status Report:")
            println(result)
            listResult = result
        } catch {
            case e: TimeoutException =>
                println("Server timeout. Request failed.")
            case e: InterruptedException =>
                println("Connection interrupted. Request failed.")
        }
        orders = listResult :: orders
    }

    private def handleEventQueryAck(eventsList: List[Event]): Unit = {
        println("Events on sale:")
        printList(eventsList)
    }

    private def handleStringMessage(msg: String): Unit = {
        println("Message: " + msg)
        println("Forwarding message to kiosk...")
        kiosk ! msg
    }

    private def handleOrderReceived(tickets: List[Ticket]): Unit = {
        println("Purchase successful!")
        orders = tickets :: orders
        printList(tickets)
    }

    private def printOrders(): Unit = {
        println("Orders:")
        orders.foreach(order => order.foreach(
            ticket => println(ticket)
        ))
    }

    private def saveOrders(): Unit = {
        println("Saving orders...")
        val json: String = upickle.default.write(orders)
        os.write(os.pwd / "orders.json", json)
    }

    /**
     * @see <a href="https://alvinalexander.com/scala/akka-actor-how-to-send-message-wait-for-reply-ask/">Wait for Reply</a>
     */
    private def handleStatusReport(): Unit = {
        try {
            val future: Future[Any] = master ? STATUS_REPORT
            val result = Await.result(future, timeout.duration).asInstanceOf[String]
            println("Status Report:")
            println(result)
        } catch {
            case e: TimeoutException =>
                println("Server timeout. Request failed.")
            case e: InterruptedException =>
                println("Connection interrupted. Request failed.")
        }
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

    private def handleKillOrder(): Unit = {
        kiosk ! SELF_DESTRUCT
    }

    private def printList(list: List[Any]): Unit = {
        list.foreach(e => println(e.toString))
    }

}

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
    private val config: Config          = ConfigFactory.load.getConfig("server")
    private val numberOfKiosks: Int     = ConfigFactory.load.getInt("server.allocation.number-of-kiosks")
    private val remoteAddress: Address  = setRemoteAddress()
//    private val system  = ActorSystem("TicketSelling", config)
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
        println("[Client] Remote address set to " + remoteAddress.toString)
        // return
        remoteAddress
    }

    override def receive: Receive = {
        case BUY(ticketQuantity: Int, title: String) =>
            handleBuy(ticketQuantity, title)
        case EVENT_DOES_NOT_EXIST(title: String) =>
            println(s"The event $title does not exist.")
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
            println("TIMEOUT: No Response")
        case SELF_DESTRUCT =>
            handleKillOrder()
        case msg: String =>
            handleStringMessage(msg)
    }

    private def handleBuy(ticketQuantity: Int, title: String): Unit = {
        kiosk ! BUY(ticketQuantity, title)
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
        printList(tickets)
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
//        master ! STATUS_REPORT
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

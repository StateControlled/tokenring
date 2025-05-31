package application.client

import akka.actor.{Actor, ActorSelection, ActorSystem, Address}
import application.core.{EVENTS_QUERY, EVENTS_QUERY_ACK, STATUS_REPORT, SWITCH}
import com.typesafe.config.{Config, ConfigFactory}

class Client extends Actor {
    private val config: Config          = ConfigFactory.load.getConfig("server")
    private val numberOfKiosks: Int     = ConfigFactory.load.getInt("server.allocation.number-of-kiosks")
    private val remoteAddress: Address  = setRemoteAddress()
    private val system  = ActorSystem("TicketSelling", config)
    private var kioskId = 1

    private val masterName  = config.getString("naming.master-actor-name")
    private val kioskName   = config.getString("naming.node-actor-name")

    var kiosk: ActorSelection   = system.actorSelection(s"$remoteAddress/user/$kioskName$kioskId")
    var master: ActorSelection  = system.actorSelection(s"$remoteAddress/user/$masterName")

    private def setRemoteAddress(): Address = {
        val hostname = ConfigFactory.load.getString("server.akka.remote.artery.canonical.hostname")
        val port = ConfigFactory.load.getInt("server.akka.remote.artery.canonical.port")

        val remoteAddress: Address = Address("akka", "TicketSelling", hostname, port)
        println("Remote address set to " + remoteAddress.toString)
        // return
        remoteAddress
    }

    override def receive: Receive = {
        case STATUS_REPORT() =>
            statusReport()
        case EVENTS_QUERY() =>
            listEvents()
        case EVENTS_QUERY_ACK(eventsList) =>
            printList(eventsList)
        case SWITCH() =>
            selectKiosk()
        case msg: String =>
            println("Received message: " + msg)
            println("Forwarding message to kiosk...")
            kiosk ! msg
    }

    private def statusReport(): Unit = {
        master ! STATUS_REPORT()
    }

    private def listEvents(): Unit = {
        master ! EVENTS_QUERY()
    }

    private def selectKiosk(): Unit = {
        kioskId = kioskId + 1
        if (kioskId > numberOfKiosks) {
            kioskId = 1
        }
        kiosk = system.actorSelection(s"$remoteAddress/user/kiosk$kioskId")
    }

    private def printList(list: List[Any]): Unit = {
        list.foreach(e => println(e.toString))
    }

}

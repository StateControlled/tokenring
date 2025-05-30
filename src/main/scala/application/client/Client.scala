package application.client

import akka.actor.{Actor, ActorSelection, ActorSystem, Address}
import application.core.{EVENTS_QUERY, EVENTS_QUERY_ACK, STATUS_REPORT}
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn.readLine

object Client extends App, Actor {
    private val config: Config          = ConfigFactory.load.getConfig("client")
    private val numberOfKiosks: Int     = ConfigFactory.load.getInt("server.allocation.number-of-kiosks")
    private val remoteAddress: Address  = setRemoteAddress()
    private val system  = ActorSystem("TicketSelling", config)
    private var kioskId = 1

    private val masterName  = config.getString("server.naming.master-actor-name")
    private val kioskName   = config.getString("server.naming.node-actor-name")

    var kiosk: ActorSelection   = system.actorSelection(s"$remoteAddress/user/$kioskName$kioskId")
    var master: ActorSelection  = system.actorSelection(s"$remoteAddress/user/$masterName")

    run()

    private def run(): Unit = {
        while (true) {
            println("Next > ")
            val command = readLine()
            try
                if (command.equalsIgnoreCase("exit")) {
                    println("Goodbye")
                    System.exit(0)
                } else if (command.equalsIgnoreCase("report")) {
                    master ! STATUS_REPORT()
                } else if (command.equalsIgnoreCase("switch")) {
                    selectKiosk()
                } else if (command.equalsIgnoreCase("list")) {
                    listEvents()
                } else if (command.equalsIgnoreCase("buy")) {
                    // TODO purchase logic
                } else if (command.equalsIgnoreCase("message") || command.equalsIgnoreCase("msg")) {
                    println("Message > ")
                    val message = readLine()
                    kiosk ! message
                } else {
                    println("Not a valid option. Please try again")
                }
            catch
                case e: Exception =>
                    e.printStackTrace()
        }
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

    private def setRemoteAddress(): Address = {
        val hostname = ConfigFactory.load.getString("server.akka.remote.artery.canonical.hostname")
        val port = ConfigFactory.load.getInt("server.akka.remote.artery.canonical.port")

        val remoteAddress: Address = Address("akka", "TicketSelling", hostname, port)
        println("Remote address set to " + remoteAddress.toString)
        // return
        remoteAddress
    }

    override def receive: Receive = {
        case EVENTS_QUERY_ACK(eventsList) =>
            printList(eventsList)
    }

    private def printList(list: List[Any]): Unit = {
        list.foreach(e => println(e.toString))
    }

}

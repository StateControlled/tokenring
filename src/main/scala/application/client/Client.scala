package application.client

import akka.actor.{ActorSelection, ActorSystem, Address}
import application.server.STATUS_REPORT
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn.readLine

object Client extends App {
    private val config: Config = ConfigFactory.load.getConfig("client")
    private val numberOfKiosks: Int = ConfigFactory.load.getInt("server.allocation.number-of-kiosks")
    private val remoteAddress: Address = setRemoteAddress()
    private val system = ActorSystem("TicketSelling", config)
    private var k = 1;

    var kiosk: ActorSelection = system.actorSelection(s"$remoteAddress/user/kiosk$k")

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
                    kiosk ! STATUS_REPORT()
                } else if (command.equalsIgnoreCase("switch")) {
                    selectKiosk()
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

    private def selectKiosk(): Unit = {
        k = k + 1
        if (k > numberOfKiosks) {
            k = 1
        }
        kiosk = system.actorSelection(s"$remoteAddress/user/kiosk$k")
    }

    private def setRemoteAddress(): Address = {
        val hostname = ConfigFactory.load.getString("server.akka.remote.artery.canonical.hostname")
        val port = ConfigFactory.load.getInt("server.akka.remote.artery.canonical.port")

        val remoteAddress: Address = Address("akka", "TicketSelling", hostname, port)
        println("Remote address set to " + remoteAddress.toString)
        // return
        remoteAddress
    }

}

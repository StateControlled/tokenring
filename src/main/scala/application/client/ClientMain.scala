package application.client

import akka.actor.{ActorSystem, Props}
import application.core.{EVENTS_QUERY, STATUS_REPORT, SWITCH}
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn.readLine

/**
 * Starts a [[Client]] Actor and sends messages to the client that are then forwarded to a [[application.server.Kiosk Kiosk]]
 * or the [[application.server.Master Master]] for processing.
 */
object ClientMain extends App {
    private val config: Config = ConfigFactory.load.getConfig("client")

    private val system = ActorSystem("TicketSelling", config)

    private val client = system.actorOf(Props[Client](), name="client")

    run()

    private def run(): Unit = {
        while (true) {
            println("Next > ")
            val command = readLine()
            try
                if (command.equalsIgnoreCase("exit")) {
                    println("Goodbye")
                    system.terminate()
                    System.exit(0)
                } else if (command.equalsIgnoreCase("report")) {
                    client ! STATUS_REPORT()
                } else if (command.equalsIgnoreCase("switch")) {
                    client ! SWITCH()
                } else if (command.equalsIgnoreCase("list")) {
                    client ! EVENTS_QUERY()
                } else if (command.equalsIgnoreCase("buy")) {
                    // TODO purchase logic
                } else if (command.equalsIgnoreCase("message")) {
                    println("Message > ")
                    val message = readLine()
                    client ! message
                } else {
                    println("Not a valid option. Please try again")
                }
            catch
                case e: Exception =>
                    e.printStackTrace()
        }
    }

}

package application.client

import akka.actor.{ActorSystem, Props}
import application.core.*
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn.readLine
import scala.util.Try

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
                    client ! SWITCH
                } else if (command.equalsIgnoreCase("list")) {
                    client ! EVENTS_QUERY()
                } else if (command.equalsIgnoreCase("buy")) {
                    // TODO purchase logic
                    sendBuyRequest()
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

    /**
     * Prompts [[Client]] to request tickets from a [[application.server.Kiosk Kiosk]]
     */
    private def sendBuyRequest(): Unit = {
        println("Event > ")
        val eventTitle = readLine()
        println("Quantity >")
        val ticketQuantity = readLine()
        val quantity = tryToInt(ticketQuantity)
        if (quantity.isDefined) {
            client ! BUY(quantity.get, eventTitle)
        }
    }

    /**
     * Attempts to parse a string to an [[Int]]. If this fails, returns [[None]].
     *
     * @param str   the string to parse
     * @return      an [[Option]] containing the [[Int]] or [[None]]
     */
    private def tryToInt(str: String): Option[Int] = {
        Try(str.toInt).toOption
    }

}

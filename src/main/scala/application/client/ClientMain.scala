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

    /**
     * The command-line interface for the Client
     */
    private def run(): Unit = {
        while (true) {
            println("Next > ")
            val command = readLine()
            try
                if (command.equalsIgnoreCase("exit")) {
                    exit()
                } else if (command.equalsIgnoreCase("report")) {
                    sendStatusQuery()
                } else if (command.equalsIgnoreCase("switch")) {
                    sendSwitchCommand()
                } else if (command.equalsIgnoreCase("list")) {
                    sendEventsQuery()
                } else if (command.equalsIgnoreCase("buy")) {
                    // TODO purchase logic
                    sendBuyRequest()
                } else if (command.equalsIgnoreCase("message")) {
                    sendString()
                } else if (command.equalsIgnoreCase("kill")) {
                    sendKillOrder()
                } else {
                    println("Not a valid option. Please try again")
                }
            catch
                case e: Exception =>
                    e.printStackTrace()
        }
    }

    private def sendString(): Unit = {
        println("Message > ")
        val message = readLine()
        client ! message
    }

    private def exit(): Unit = {
        println("Goodbye")
        system.terminate()
        System.exit(0)
    }

    private def sendEventsQuery(): Unit = {
        client ! EVENTS_QUERY()
    }

    private def sendSwitchCommand(): Unit = {
        client ! SWITCH
    }

    private def sendStatusQuery(): Unit = {
        client ! STATUS_REPORT()
    }

    private def sendKillOrder(): Unit = {
        client ! SELF_DESTRUCT
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
        } else {
            println("Could not parse command.")
            println("Please try again.")
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

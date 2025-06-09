package application.client

import akka.actor.{ActorSystem, Props}
import application.core.*
import application.core.CommandParser.CommandType
import application.core.CommandParser.CommandType.*
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

    /**
     * The command-line interface for the Client
     */
    private def run(): Unit = {
        while (true) {
            menu()
            println("Next > ")
            val command = readLine()
            try
                val commandType: CommandParser.CommandType = CommandParser.parse(command)

                commandType match {
                    case HELP => menu()
                    case EXIT => exit()
                    case NEXT => sendSwitchCommand()
                    case LIST => sendEventsQuery()
                    case PURCHASE => sendBuyRequest()
                    case ORDERS => sendListOrderRequest()
                    case SAVE => saveOrders()
                    case _ => println("Not a valid option. Please try again")
                }
            catch
                case e: Exception =>
                    e.printStackTrace()
        }
    }

    /**
     * Prints the menu by retrieving all the values in the [[CommandParser.CommandType]] enum and printing those.
     */
    private def menu(): Unit = {
        println("--Enter next command")
        commands()
    }

    private def help(): Unit = {
        println("--Help")
        commands()
    }

    private def commands(): Unit = {
        CommandParser.CommandType.values
            .filter(ord => ord != CommandType.NO_ACTION) // don't include irrelevant options
            .foreach(ord => println(f"${ord.name.toUpperCase}%-8s\t- ${ord.description}%s"))
        println()
    }

    private def exit(): Unit = {
        println("Goodbye")
        system.terminate()
        System.exit(0)
    }

    /**
     * Prompts [[Client]] to request tickets from a [[application.server.Kiosk Kiosk]]
     */
    private def sendBuyRequest(): Unit = {
        println("Event > ")
        val eventTitle = readLine()
        client ! BUY(eventTitle)
    }

    private def sendSwitchCommand(): Unit = {
        client ! SWITCH
    }

    private def sendEventsQuery(): Unit = {
        client ! EVENTS_QUERY()
    }

    private def sendListOrderRequest(): Unit = {
        client ! PRINT_ORDERS
    }

    private def saveOrders(): Unit = {
        client ! SAVE_ORDERS
    }
    
}

package application.server

import akka.actor.{ActorSystem, Props}
import application.core.*
import application.core.CommandParser.CommandType.*
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source
import scala.io.StdIn.readLine

/**
 * Initializes [[Event Events]] then initializes a [[Master]] which will, in turn, initialize [[Kiosk Kiosks]]
 */
object Server extends App {
    private val config: Config  = ConfigFactory.load.getConfig("server")
    private var tokenId         = config.getInt("token.token-id")
    private val masterName      = config.getString("naming.master-actor-name")

    // Read events from file then send to Master for allocation.
    /** List of all [[Event Events]] */
    private val eventList = readData()
    eventList.foreach(event => println(event))

    //**********************************************************************************//
    
    private val system = ActorSystem("TicketSelling", config)
    private val master = system.actorOf(Props(classOf[Master], 0, eventList), name=s"$masterName")

    Thread.sleep(2000)

    run()

    /**
     * Command line logic for server
     */
    private def run(): Unit = {
        while (true) {
            println("Next > ")
            val command = readLine()

            try
                val commandType: CommandParser.CommandType = CommandParser.parse(command)

                commandType match {
                    case EXIT => exit()
                    case LIST => listChunks()
                    case _ => println("Not a valid option. Please try again")
                }
            catch
                case e: Exception => e.printStackTrace()
        }
    }

    private def exit(): Unit = {
        stop()
        shutdown()
    }

    private def stop(): Unit = {
        master ! STOP
    }

    private def shutdown(): Unit = {
        System.exit(0)
    }
    
    private def listChunks(): Unit = {
        master ! LIST_CHUNKS
    }

    /**
     * Reads the json file that contains event data from /resources
     *
     * @return  a List of Events
     */
    private def readData(): List[Event] = {
        // reads from src/main/resources
        val resource = Source.fromResource("events.json")

        // file contents to string
        val builder: StringBuilder = new StringBuilder()
        resource.getLines().foreach(line => builder.append(line))
        val jsonString: String = builder.toString()

        // deserialize json string
        val list: List[Event] = upickle.default.read[List[Event]](jsonString)
        println("[Server] Loaded events from disk")
        list
    }

}

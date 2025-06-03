package application.server

import akka.actor.{ActorSystem, Props}
import application.core.*
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.StdIn.readLine
import scala.util.Random

/**
 * Initializes [[Event Events]] then initializes a [[Master]] which will, in turn, initialize [[Kiosk Kiosks]]
 */
object Server extends App {
    private val config: Config  = ConfigFactory.load.getConfig("server")
    private var tokenId         = config.getInt("token.token-id")
    private val masterName      = config.getString("naming.master-actor-name")

    private val concertHall     = Venue("Orchestra Hall", 240)
    private val libertyStadium  = Venue("Liberty Stadium", 300)
    private val greenField      = Venue("Green Field", 360)
    private val outdoorStage    = Venue("Outdoor Stage", 360)

    /** List of all [[Venue]] */
    private val venueList = List(outdoorStage, greenField, libertyStadium, concertHall)

    private val boyBand         = Event("BoyBand Concert", libertyStadium, randomDate())
    private val kpop            = Event("Kpop Concert", libertyStadium, randomDate())
    private val charityRun      = Event("Charity Fun Run", greenField, randomDate())
    private val sportsGame      = Event("Sports Game", greenField, randomDate())
    private val classicalMusic  = Event("Symphony Concert", concertHall, randomDate())
    private val violinHero      = Event("Famous Violinist Plays", concertHall, randomDate())
    private val rockMusic       = Event("Awesome Band Concert", outdoorStage, randomDate())
    private val jazzBand        = Event("Smooth Jazz", outdoorStage, randomDate())

    /** List of all [[Event]] */
    private val eventList = List(boyBand, kpop, charityRun, sportsGame, classicalMusic, violinHero, rockMusic, jazzBand)

    //**********************************************************************************//
    
    private val system = ActorSystem("TicketSelling", config)
    private val master = system.actorOf(Props(classOf[Master], 0, venueList, eventList), name=s"$masterName")

    Thread.sleep(5000)

    run()

    /**
     * Command line logic for server
     */
    private def run(): Unit = {
        while (true) {
            println("Next > ")
            val command = readLine()
            try
                if (command.equalsIgnoreCase("stop")) {
                    master ! STOP
                } else if (command.equalsIgnoreCase("exit")) {
                    System.exit(0)
                } else if (command.equalsIgnoreCase("start")) {
                    val token = TOKEN(tokenId)
                    master ! START(token)
                    tokenId = tokenId + 1
                } else if (command.equalsIgnoreCase("report")) {
                    master ! STATUS_REPORT
                } else {
                    println("Not a valid option. Please try again")
                }
            catch
                case e: Exception => e.printStackTrace()
        }
    }

    /**
     * Generates a random date.
     *
     * @return  a string representation of a random date
     */
    private def randomDate(): String = {
        val month: Int = new Random().nextInt(11) + 1
        val day: Int = new Random().nextInt(31) + 1
        val year: Int = new Random().nextInt(2) + 2025
        String.format("%04d-%02d-%02d", year, month, day)
    }

}

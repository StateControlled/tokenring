package application.server

import akka.actor.{ActorSystem, Props}
import application.core.{CONCERT_HALL, Event, OUTDOOR, OUTDOOR_CONCERT_HALL, STADIUM, Venue}
import com.typesafe.config.ConfigFactory

import scala.io.StdIn.readLine
import scala.util.Random

object TokenRing extends App {
    private val config = ConfigFactory.load()
    private var tokenId = config.getInt("server.token.token-id")
//    private val numberOfKiosks = config.getInt("server.allocation.number-of-kiosks")

    private val concertHall     = new Venue("Orchestra Hall", 240, CONCERT_HALL())
    private val libertyStadium  = new Venue("Liberty Stadium", 300, STADIUM())
    private val greenField      = new Venue("Green Field", 360, OUTDOOR())
    private val outdoorStage    = new Venue("Outdoor Stage",360, OUTDOOR_CONCERT_HALL())

    /** List of all [[Venue]] */
    private val venueList = List(outdoorStage, greenField, libertyStadium, concertHall)

    private val boyBand         = new Event("BoyBand Concert", libertyStadium, randomDate())
    private val kpop            = new Event("Kpop Concert", libertyStadium, randomDate())
    private val charityRun      = new Event("Charity Fun Run", greenField, randomDate())
    private val sportsGame      = new Event("Sports Game", greenField, randomDate())
    private val classicalMusic  = new Event("Symphony Concert", concertHall, randomDate())
    private val violinHero      = new Event("Famous Violinist Plays", concertHall, randomDate())
    private val rockMusic       = new Event("Awesome Band Concert", outdoorStage, randomDate())
    private val jazzBand        = new Event("Smooth Jazz", outdoorStage, randomDate())

    /** List of all [[Event]] */
    private val eventList = List(boyBand, kpop, charityRun, sportsGame, classicalMusic, violinHero, rockMusic, jazzBand)

    //**********************************************************************************//
    
    private val system = ActorSystem("TicketSelling")
    private val master = system.actorOf(Props(classOf[Master], 0, venueList, eventList), name="master")

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
                    master ! Stop
                } else if (command.equalsIgnoreCase("exit")) {
                    System.exit(0)
                } else if (command.equalsIgnoreCase("start")) {
                    val token = Token(tokenId)
                    master ! Start(token)
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

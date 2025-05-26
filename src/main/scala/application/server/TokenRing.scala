package application.server

import akka.actor.{ActorRef, ActorSystem, Props}
import application.core.{CONCERT_HALL, Event, OUTDOOR, OUTDOOR_CONCERT_HALL, STADIUM, Venue}
import com.typesafe.config.ConfigFactory

import scala.util.Random

object TokenRing extends App {
    private val config = ConfigFactory.load()
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
//    private var nodes: List[ActorRef] = List.empty
//
//    private val node04 = system.actorOf(Props(classOf[Kiosk], 4), name="node4")
//    private val node03 = system.actorOf(Props(classOf[Kiosk], 3), name="node3")
//    private val node02 = system.actorOf(Props(classOf[Kiosk], 2), name="node2")
//    private val node01 = system.actorOf(Props(classOf[Kiosk], 1), name="node1")
    private val master = system.actorOf(Props(classOf[Master], 0, venueList, eventList), name="master")
//
//    nodes = node04 :: node03 :: node02 :: node01 :: nodes

    // Tell all actors what their neighbor node is
//    master ! SetNextNode(node01)
//    node01 ! SetNextNode(node02)
//    node02 ! SetNextNode(node03)
//    node03 ! SetNextNode(node04)
//    node04 ! SetNextNode(master)

    Thread.sleep(5000)

    private val token = Token(255)

    master ! Start(token)

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

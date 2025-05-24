import akka.actor.{ActorSystem, Props}
import server.{Master, Kiosk, SetNextNode, Start, Token}
import ticket.{Event, Venue}

object TokenRing extends App {
    private val concertHall = new Venue("Orchestra Hall", 50)
    private val libertyStadium = new Venue("Liberty Stadium", 100)
    private val greenField = new Venue("Green Field", 250)
    private val outdoorStage = new Venue("Outdoor Stage", 250)

    /** List of all [[Venue]] */
    private val venueList = List(outdoorStage, greenField, libertyStadium, concertHall)

    private val boyBand = new Event(libertyStadium)
    private val kpop = new Event(libertyStadium)
    private val charityRun = new Event(greenField)
    private val sportsGame = new Event(greenField)
    private val classicalMusic = new Event(concertHall)
    private val violinHero = new Event(concertHall)
    private val rockMusic = new Event(outdoorStage)
    private val jazzBand = new Event(outdoorStage)

    /** List of all [[Event]] */
    private val eventList = List(boyBand, kpop, charityRun, sportsGame, classicalMusic, violinHero, rockMusic, jazzBand)

    //**********************************************************************************//
    
    private val system = ActorSystem("TicketSelling")

    private val node03 = system.actorOf(Props(classOf[Kiosk], 3), name="node3")
    private val node02 = system.actorOf(Props(classOf[Kiosk], 2), name="node2")
    private val node01 = system.actorOf(Props(classOf[Kiosk], 1), name="node1")
    private val master = system.actorOf(Props(classOf[Master], 0, venueList, eventList), name="master")

    master ! SetNextNode(node01)
    node01 ! SetNextNode(node02)
    node02 ! SetNextNode(node03)
    node03 ! SetNextNode(master)

    Thread.sleep(5000)

    private val token = Token(255)

    master ! Start(token)

}

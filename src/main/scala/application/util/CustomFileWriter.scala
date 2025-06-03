package application.util

import application.core.*

object CustomFileWriter extends App {
    private val concertHall = Venue("Orchestra Hall", 240)
    private val libertyStadium = Venue("Liberty Stadium", 300)
    private val greenField = Venue("Green Field", 360)
    private val outdoorStage = Venue("Outdoor Stage", 360)

    /** List of all [[Venue]] */
    private val venueList = List(outdoorStage, greenField, libertyStadium, concertHall)

    private val boyBand = Event("BoyBand Concert", libertyStadium, "2025-08-16")
    private val kpop = Event("Kpop Concert", libertyStadium, "2025-09-25")
    private val charityRun = Event("Charity Fun Run", greenField, "2025-10-26")
    private val sportsGame = Event("Sports Game", greenField, "2025-11-18")
    private val classicalMusic = Event("Symphony Concert", concertHall, "2026-01-15")
    private val violinHero = Event("Famous Violinist Plays", concertHall, "2026-02-14")
    private val rockMusic = Event("Awesome Band Concert", outdoorStage, "2026-03-31")
    private val jazzBand = Event("Smooth Jazz", outdoorStage, "2026-04-16")

    /** List of all [[Event]] */
    private val eventList = List(boyBand, kpop, charityRun, sportsGame, classicalMusic, violinHero, rockMusic, jazzBand)

    writeData()

    private def writeData(): Unit = {
        val json: String = upickle.default.write(eventList)
        os.write(os.pwd / "events.json", json)
    }

}

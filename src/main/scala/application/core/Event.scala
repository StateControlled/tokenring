package application.core

import scala.util.Random

/**
 * @param name  the name of the event
 * @param venue the [[Venue]] this event is held in
 * @param date  the date the [[Event]] will occur
 */
class Event(val name: String, val venue: Venue, val date: String) {
    private val id: String = uuid()
    /** The number of tickets sold */
    private var occupancy: Int = 0
    /** The maximum number of tickets that can be sold */
    private val capacity = venue.getCapacity

    /**
     * @return  the number of tickets that have been sold
     */
    def ticketsSold: Int = {
        occupancy
    }

    /**
     * @return  the number of tickets remaining to be sold
     */
    def openSeats: Int = {
        capacity - occupancy
    }

    /**
     * Adjust the number of tickets sold by the given amount. If the number of tickets in the purchase exceeds the number of
     * tickets for sale, the operation will not succeed.
     *
     * @param amount    the number of tickets to sell
     * @return          a [[List]] of tickets, if tickets were sold
     */
    def sell(amount: Int): Option[List[Ticket]] = {
        if ((occupancy + amount) < capacity) {
            occupancy = occupancy + amount
            val tickets = (1 to amount).toList.map(i => new Ticket(venue.name, name, date))
            Some(tickets)
        } else {
            Option.empty
        }
    }

    /**
     * @return <code>true</code> if this Event has reached capacity
     */
    def isFull: Boolean = {
        occupancy >= capacity
    }

    /**
     * The capacity is the maximum number of tickets that can be sold for this Event
     * @return  the capacity
     */
    def getCapacity: Int = {
        capacity
    }

    def getName: String = {
        name
    }

    def getVenue: Venue = {
        venue
    }

    def getEventDate: String = {
        date
    }

    private def uuid(): String = {
        val num = BigInt(24, Random())
        val str1: String = f"$num%06x"
        s"EV$str1"
    }

}

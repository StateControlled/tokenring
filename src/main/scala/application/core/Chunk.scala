package application.core

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the initial number of tickets allocated to this chunk
 * @param section       an identifier for this chunk
 */
class Chunk(val event: Event, var allocation: Int, var section: String) {
    private var ticketsSold = 0
    private var ticketsRemain = allocation

    /**
     * Returns true if there is a ticket to take, false if not. Decrements the number of tickets available if a ticket is taken.
     *
     * @return <code>true</code> if there is a ticket to take
     */
    def sellOne(): Boolean = {
        if (ticketsRemain > 1) {
            ticketsSold = ticketsSold + 1
            ticketsRemain = ticketsRemain - 1
            println(s"Took ${1} ticket, $ticketsRemain tickets remain.")
            true
        } else {
            println("No tickets to take")
            false
        }
    }

    /**
     * Removes an amount of tickets from the chunk. Removes the amount requested or the maximum number of available tickets,
     * if there are fewer tickets than requested.
     *
     * @param amount    the number of tickets to remove
     * @return          the number of tickets actually removed.
     */
    def take(amount: Int): Int = {
        if (amount < ticketsRemain) {
            ticketsSold = ticketsSold + amount
            ticketsRemain = ticketsRemain - amount
            amount
        } else {
            // ticketsRemain < amount
            val result = ticketsRemain
            ticketsSold = ticketsSold + ticketsRemain
            ticketsRemain = 0
            result
        }
    }

    def add(amount: Int): Boolean = {
        ticketsRemain = ticketsRemain + amount
        false
    }
    
    def getEventName: String = {
        event.name
    }

    def getVenue: Venue = {
        event.venue
    }

    def getVenueName: String = {
        event.venue.name
    }

    def getEventDate: String = {
        event.date
    }

    /**
     * The number of tickets remaining in this chunk is <code> tickets_allocated - tickets_sold</code>
     *
     * @return  the number of tickets remaining in the chunk
     */
    def getTicketsRemaining: Int = {
        ticketsRemain
    }

    def getTicketsSold: Int = {
        ticketsSold
    }

    def getSection: String = {
        section
    }

    def setSection(newSection: String): Unit = {
        section = newSection
    }

    override def toString: String = {
        f"Chunk Info: Event: ${event.name}%nInitial allocation: $allocation%nTickets remaining: $ticketsRemain%nTickets sold: $ticketsSold"
    }

}

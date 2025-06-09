package application.core

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the initial number of tickets allocated to this chunk
 */
class Chunk(val event: Event, val allocation: Int) {
    private var ticketsSold = 0
    private var ticketsRemain = allocation
    // track if the event as a whole, not just this chunk, is sold out
    private var totallySoldOut = false

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
            ticketsRemain = ticketsRemain - amount
            ticketsSold = ticketsSold + amount
            amount
        } else {
            // ticketsRemain < amount
            val result = ticketsRemain
            ticketsSold = ticketsSold + ticketsRemain
            ticketsRemain = 0
            result
        }
    }

    /**
     * Add tickets to the chunk.
     *
     * @param amount    the number of tickets to add
     * @return          the number of tickets remaining including the addition
     */
    def add(amount: Int): Int = {
        ticketsRemain = ticketsRemain + amount
        setIsTotallySoldOut(false)
        ticketsRemain
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

    /**
     * @return  <code>true</code> if there are no tickets remaining in this chunk
     */
    def isDepleted: Boolean = {
        ticketsRemain == 0
    }

    def isTotallySoldOut: Boolean = {
        totallySoldOut
    }

    def setIsTotallySoldOut(bool: Boolean): Unit = {
        totallySoldOut = bool
    }
    
    override def toString: String = {
        f"Chunk Info: Event: ${event.name}%nInitial allocation: $allocation%nTickets remaining: $ticketsRemain%nTickets sold: $ticketsSold%nSold out: $isTotallySoldOut"
    }

}

package application.core

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the initial number of tickets allocated to this chunk
 * @param section       an identifier for this chunk
 */
class Chunk(val event: Event, var allocation: Int, val section: String) {
    private var ticketsSold = 0
    private var ticketsRemain = allocation
    private var allocated: Boolean = false

    /**
     * Returns true if there is a ticket to take, false if not.
     *
     * @return <code>true</code> if there is a ticket to take
     */
    def take(): Boolean = {
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

    override def toString: String = {
        f"Chunk Info: Event: ${event.name}%nInitial allocation: $allocation%nTickets remaining: $ticketsRemain%nTickets sold: $ticketsSold"
    }

    /**
     * @return <code>true</code> if this [[Chunk]] has not been allocated to a [[Kiosk]]
     */
    def isFree: Boolean = {
        !allocated
    }

    def setIsAllocated(isAllocated: Boolean): Unit = {
        allocated = isAllocated
    }

}

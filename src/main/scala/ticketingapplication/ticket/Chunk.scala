package ticketingapplication.ticket

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the number of tickets allocated to this chunk
 */
class Chunk(val event: Event, var allocation: Int) {
    private var ticketsSold = 0

    /**
     * Adjusts the number of tickets sold by the specified amount. <br>
     * A negative number will reduce the number of tickets sold to a minimum of zero.
     *
     * @param amount    the number of tickets to sell
     * @return          <code>true</code> if there are enough tickets in the portion to complete the transaction.
     */
    def take(amount: Int): Boolean = {
        if (ticketsSold + amount < allocation) {
            if (ticketsSold + amount < 0) {
                ticketsSold = 0
            } else {
                ticketsSold = ticketsSold + amount
            }
            true
        } else {
            false
        }
    }

    /**
     * @return  the [[Event]] this chunk of tickets is for
     */
    def getVenue: Event = {
        event
    }

    /**
     * The number of tickets remaining in this chunk is <code> tickets_allocated - tickets_sold</code>
     *
     * @return  the number of tickets remaining in the chunk
     */
    def ticketsRemaining: Int = {
        allocation - ticketsSold
    }

    def getTicketsSold: Int = {
        ticketsSold
    }

}

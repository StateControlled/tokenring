package application.core

import scala.util.Random

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the number of tickets allocated to this chunk
 */
class Chunk(val event: Event, var allocation: Int) {
    private val id = uuid()
    private var ticketsSold = 0
    private var allocated: Boolean = false

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

    override def toString: String = {
        s"Chunk $id: Allocated $allocation tickets for event \"${event.getName}\""
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

    private def uuid(): String = {
        val num = BigInt(40, Random())
        val str: String = f"$num%012x"
        s"C-${str.substring(0, 4)}-${str.substring(4)}"
    }

}

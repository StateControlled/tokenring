package application.core

import scala.util.Random

/**
 * A chunk of tickets that may or may not be the total number of possible tickets on sale for an [[Event]]
 *
 * @param event         the [[Event]]
 * @param allocation    the initial number of tickets allocated to this chunk
 */
class Chunk(val event: Event, var allocation: Int) {
    private val id = uuid()
    private var ticketsSold = 0
    private var ticketsRemain = allocation
    private var allocated: Boolean = false

    /**
     * Adjusts the number of tickets sold by the specified amount. Returns the number of tickets taken from this chunk:
     * this is the amount, if enough tickets remain, or if there are not enough tickets, it returns the number of tickets
     * actually taken.
     *
     * @param amount    the number of tickets to take
     * @return          the number of tickets taken from this chunk
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

    /**
     * @return  the [[Event]] this chunk of tickets is for
     */
    def getEvent: Event = {
        event
    }

    def getVenue: Venue = {
        event.venue
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

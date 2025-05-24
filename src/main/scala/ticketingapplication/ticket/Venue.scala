package ticketingapplication.ticket

/**
 * A Venue is a location where an [[Event]] is held.
 *
 * @param name      the venue name
 * @param capacity  the maximum capacity of the venue
 */
class Venue(val name: String, val capacity: Int) {
    private var occupancy: Int = 0

    def adjustOccupancy(amount: Int): Boolean = {
        if ((occupancy + amount) < capacity) {
            occupancy = occupancy + amount
            true
        } else {
            false
        }
    }

    /**
     * @return  <code>true</code> if this Venue has reached capacity
     */
    def isFull: Boolean = {
        occupancy >= capacity
    }

    /**
     * @return  the maximum capacity of this Venue
     */
    def getCapacity: Int = {
        capacity
    }

    /**
     * Returns the occupancy, or how full the Venue is.
     *
     * @return  the current occupancy
     */
    def getOccupancy: Int = {
        occupancy
    }

}

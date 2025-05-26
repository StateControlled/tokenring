package application.core

/**
 * A Venue is a location where an [[Event]] is held.
 *
 * @param name      the venue name
 * @param capacity  the maximum capacity of the venue
 */
class Venue(val name: String, val capacity: Int, val venueType: Classification) {
    /**
     * @return  the maximum capacity of this [[Venue]]
     */
    def getCapacity: Int = {
        capacity
    }

    /**
     * @return  the name of this [[Venue]]
     */
    def getName: String = {
        name
    }

    def getClassification: Classification = {
        venueType
    }

}

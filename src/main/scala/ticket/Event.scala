package ticket

class Event(val venue: Venue) {

    def getVenue: Venue = {
        venue
    }

    def getVenueCapacity: Int = {
        venue.capacity
    }

    def getVenueOccupancy: Int = {
        venue.getOccupancy
    }

}

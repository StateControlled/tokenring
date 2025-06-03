package application.core

/**
 * @param name  the name of the event
 * @param venue the [[Venue]] this event is held in
 * @param date  the date the [[Event]] will occur
 */
case class Event(name: String, venue: Venue, date: String) {
    def getCapacity: Int = {
        venue.capacity
    }
}

package application.core

import upickle.default.ReadWriter

/**
 * @param name  the name of the event
 * @param venue the [[Venue]] this event is held in
 * @param date  the date the [[Event]] will occur
 */
case class Event(name: String, venue: Venue, date: String) derives ReadWriter {
    def getCapacity: Int = {
        venue.capacity
    }

    override def toString: String = {
        s"EVENT NAME: $name, LOCATION: ${venue.name}, DATE: $date"
    }

}

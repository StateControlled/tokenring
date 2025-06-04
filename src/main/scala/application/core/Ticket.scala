package application.core

import upickle.default.ReadWriter

/**
 * A ticket for an [[Event]]. A ticket is created at the end of a successful purchase.
 *
 * @param venue     the [[Venue]]
 * @param event     the [[Event]]
 * @param date      the date of the event
 */
case class Ticket(venue: String, event: String, date: String, seat: String) derives ReadWriter {

    override def toString: String = {
        s"$event: $date at $venue. Seat No $seat"
    }

}

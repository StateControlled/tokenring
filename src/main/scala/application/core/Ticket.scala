package application.core

import scala.util.Random

/**
 * A ticket for an [[Event]].
 *
 * @param venue     the [[Venue]]
 * @param event     the [[Event]]
 * @param date      the date of the event
 */
class Ticket(val venue: String, val event: String, val date: String) {
    private val seat: String = setSeat()

    override def toString: String = {
        s"$event: $date at $venue. Seat $seat"
    }

    private def setSeat(): String = {
        val first = new Random().nextInt(0xff)
        s"${Integer.toHexString(first)}"
    }

}

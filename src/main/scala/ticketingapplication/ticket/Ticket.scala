package ticketingapplication.ticket

/**
 * A ticket for an [[Event]].
 *
 * @param venue     the [[Venue]]
 * @param event     the [[Event]]
 * @param date      the date of the event
 * @param seatNo    a seat identifier
 */
class Ticket(val venue: String, val event: String, val date: String, val seatNo: Int) {

}

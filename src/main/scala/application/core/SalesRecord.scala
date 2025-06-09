package application.core

/**
 * A record of sales for a particular event for a given Kiosk.
 *
 * @param kioskId   the id of the seller
 * @param event     the event title
 * @param sales     the number of tickets sold by this Kiosk
 * @param open      the number of tickets remaining
 */
case class SalesRecord(kioskId: Int, event: String, var sales: Int, var open: Int) {

    override def toString: String = {
        f"Kiosk ID: $kioskId, Event: $event%nSold $sales tickets, $open tickets remaining"
    }

}

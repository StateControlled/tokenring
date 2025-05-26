package ticketingapplication.server

import ticketingapplication.ticket.{Event, Venue}

/**
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @param venues
 * @param events
 */
class Master(override val id : Int, venues: List[Venue], events: List[Event]) extends Kiosk(id) {


    override def receive: Receive = {
        case SetNextNode(node) =>
            nextNode = node
        case token: Token =>
            println(s"server.Master $id received token ${token.id}")
        case Start(token: Token) =>
            nextNode ! token
    }

}

package server

import ticket.{Event, Venue}

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

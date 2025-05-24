package ticketingapplication.server

import akka.actor.{Actor, ActorRef}

/**
 * A <code>Node</code> in the token ring system.
 *
 * @param id a unique id to identify this [[Kiosk]]
 * @see [[Master]]
 */
class Kiosk(val id : Int) extends Actor {
    var nextNode: ActorRef = _

    override def receive: Receive = {
        case SetNextNode(node) =>
            nextNode = node
        case token: Token =>
            println(s"server.Kiosk $id received token ${token.id}")
            process()
            nextNode ! token
        case Stop =>
            // stop
    }

    private def process(): Unit = {
        Thread.sleep(500) // Simulate processing time
    }

}

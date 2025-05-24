package ticketingapplication.server

import akka.actor.ActorRef
import ticketingapplication.ticket.Ticket

/**
 * Base for all messages which can be passed to a [[Kiosk]].
 *
 * @see <a href="https://www.tutorialspoint.com/scala/scala_sealed_trait.htm">Sealed Trait</a> <br>
 *      <a href="https://docs.scala-lang.org/tour/case-classes.html">Case Class</a>
 */
sealed trait Message

/**
 * A token that is to be passed around the ring. Only Actors with the token may execute certain operations.
 * @param id    a unique id to identify this token
 */
case class Token(id: Int) extends Message

/**
 * Specifically for setting a [[Kiosk]]'s neighbor.
 *
 * @param nextNode the next [[Kiosk]] in the ring
 */
case class SetNextNode(nextNode: ActorRef) extends Message

case class TicketData(ticket: Ticket) extends Message

/**
 * Start with a given [[Message]].
 * @param msg   the message to pass
 */
case class Start(msg: Message) extends Message

case class Stop() extends Message

package application.core

import akka.actor.ActorRef

/**
 * Base for all messages which can be passed to a [[application.server.Kiosk]].
 *
 * @see <a href="https://www.tutorialspoint.com/scala/scala_sealed_trait.htm">Sealed Trait</a> <br>
 *      <a href="https://docs.scala-lang.org/tour/case-classes.html">Case Class</a>
 */
sealed trait Message

/**
 * A token that is to be passed around the ring. Only Actors with the token may execute certain operations.
 * @param id    a unique id to identify this token
 */
case class TOKEN(id: Int) extends Message

/**
 * Specifically for setting a [[application.server.Kiosk]]'s neighbor.
 *
 * @param nextNode the next [[application.server.Kiosk]] in the ring
 */
case class SET_NEXT_NODE(nextNode: ActorRef) extends Message
case class SET_MASTER(master: ActorRef) extends Message

/**
 * Specifically for allocating a chunk of tickets to a [[application.server.Kiosk]]
 *
 * @param chunk the chunk to allocate to the [[application.server.Kiosk]]
 */
case class ALLOCATE_CHUNK(chunk: Chunk) extends Message

/**
 * Start with a given [[Message]].
 * @param msg   the message to pass
 */
case class START(msg: Message) extends Message

/**
 * A simple Stop message
 */
case object STOP extends Message

/**
 * A message that indicates a client would like to purchase tickets for the given event.
 *
 * @param ticketQuantity    the desired number of tickets
 * @param eventName             the event
 */
case class BUY(ticketQuantity: Int, eventName: String) extends Message

/**
 * For a [[application.server.Kiosk]] to alert the [[application.server.Master]] that it needs more tickets for an [[Event]]
 *
 * @param event the event
 */
case class NEED_MORE_TICKETS(event: Event) extends Message

case object STATUS_REPORT extends Message
case class STATUS_REPORT_ACK(response: String) extends Message

case class EVENTS_QUERY() extends Message
case class EVENTS_QUERY_ACK(events: List[Event]) extends Message

case object SWITCH extends Message

case class EVENT_DOES_NOT_EXIST(title: String) extends Message

/**
 * An order containing the tickets from a purchase.
 *
 * @param order a list of tickets
 */
case class ORDER(order: List[Ticket]) extends Message

case object SELF_DESTRUCT extends Message
case object PRINT_ORDERS extends Message
case object SAVE_ORDERS extends Message

package application.core

import akka.actor.ActorRef

import scala.collection.mutable

/**
 * Base trait for all messages which can be passed to a [[application.server.Kiosk]].
 * @see <a href="https://www.tutorialspoint.com/scala/scala_sealed_trait.htm">Sealed Trait</a> <br>
 *      <a href="https://docs.scala-lang.org/tour/case-classes.html">Case Class</a>
 */
sealed trait Message

/**
 * Specifically for setting a [[application.server.Kiosk]]'s neighbor.
 * @param nextNode the next [[application.server.Kiosk]] in the ring
 */
case class SET_NEXT_NODE(nextNode: ActorRef) extends Message
case class SET_MASTER(master: ActorRef) extends Message

/** For distributing tickets to Kiosks */
case class ALLOCATE_CHUNKS(var chunk: List[Chunk], chunkSize: Int, destinationId: Int) extends Message
case class SALES_REPORT(events: mutable.Map[Event, Boolean]) extends Message

/** A simple Stop message to trigger coordinated shutdown of the system */
case object STOP extends Message

/**
 * A message that indicates a client would like to purchase tickets for the given event.
 * @param eventName             the event
 */
case class BUY(eventName: String) extends Message

/**
 * An order containing the tickets from a purchase.
 * @param order a list of tickets
 */
case class ORDER(order: Ticket) extends Message

/**
 * For a [[application.server.Kiosk]] to alert the [[application.server.Master]] that it needs more tickets for an [[Event]]
 * @param event the event
 */
case class NEED_MORE_TICKETS(event: Event, replyTo: ActorRef) extends Message
case class TICKET_ASK(event: Event, replyTo: ActorRef) extends Message
case class TICKET_ASK_REPLY(chunk: Chunk) extends Message

case class EVENTS_QUERY() extends Message
case class EVENTS_QUERY_ACK(events: List[Event]) extends Message
case class EVENT_SOLD_OUT(event: Event, tryAgain: Boolean) extends Message
case class EVENT_DOES_NOT_EXIST(title: String) extends Message

case object SWITCH extends Message
case object PRINT_ORDERS extends Message
case object SAVE_ORDERS extends Message
case object LIST_CHUNKS extends Message

case class GENERIC_RESPONSE(message: String) extends Message

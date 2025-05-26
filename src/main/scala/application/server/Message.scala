package application.server

import akka.actor.ActorRef
import application.core.{Chunk, Event, Ticket}

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

/**
 * Specifically for allocating a chunk of tickets to a [[Kiosk]]
 *
 * @param chunk the chunk to allocate to the [[Kiosk]]
 */
case class SetChunk(chunk: Chunk) extends Message

/**
 * A list of [[Chunk]]s to allocate to [[Kiosk]]s
 *
 * @param tickets   [[Chunks]] to allocate
 */
case class TicketData(tickets: List[Chunk]) extends Message

/**
 * Start with a given [[Message]].
 * @param msg   the message to pass
 */
case class Start(msg: Message) extends Message

/**
 * A Stop message
 */
case class Stop() extends Message

/**
 * A message that indicates a client would like to purchase tickets for the given event.
 *
 * @param tickets   the desired number of tickets
 * @param event     the event
 */
case class Buy(tickets: Int, event: Event) extends Message

/**
 * Order confirmation to send back to a client.
 *
 * @param order a list of purchased tickets
 */
case class Order(order: Option[List[Ticket]]) extends Message

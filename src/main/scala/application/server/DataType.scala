package application.server

import application.core.Event

sealed trait DataType

case class NEIGHBOR() extends DataType
case class ON_SALE() extends DataType
case class IDENTITY() extends DataType
case class CAN_SELL_QUERY(event: Event) extends DataType
case class CAN_SELL_ACK(event: Event, canSell: Boolean) extends DataType

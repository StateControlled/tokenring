package application.core

/**
 * Allows for classification of [[Venue]]s for identification purposes.
 */
sealed trait Classification

case class OUTDOOR() extends Classification
case class INDOOR() extends Classification
case class STADIUM() extends Classification
case class THEATER() extends Classification
case class CONCERT_HALL() extends Classification
case class OUTDOOR_CONCERT_HALL() extends Classification

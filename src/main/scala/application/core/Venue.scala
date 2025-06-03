package application.core

import upickle.default.ReadWriter

/**
 * A Venue is a location where an [[Event]] is held.
 *
 * @param name      the venue name
 * @param capacity  the maximum capacity of the venue
 */
case class Venue(name: String, capacity: Int) derives ReadWriter

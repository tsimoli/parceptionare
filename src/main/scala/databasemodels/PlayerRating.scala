package databasemodels

import org.joda.time.DateTime

case class PlayerRating(id: Long, steamId: String, steamId64bit: Long, rating: Float, demoId: Long, created: DateTime)

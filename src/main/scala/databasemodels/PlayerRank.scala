package databasemodels

import org.joda.time.DateTime

case class PlayerRank(id: Long, steamId: String, steamId64bit: Long, mmMatchesWon: Int,rank: Int, demoId: Long, matchDate: DateTime, created: DateTime)

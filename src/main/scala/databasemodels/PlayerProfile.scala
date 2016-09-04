package databasemodels

import org.joda.time.DateTime

case class PlayerProfile(
                          id: Long,
                          steamId: String,
                          steam64bit: Long,
                          name: Option[String],
                          playedGames: Long,
                          playedRounds: Long,
                          gamesWon: Long,
                          gamesTied: Long,
                          roundsWon: Long,
                          kills: Long,
                          deaths: Long,
                          assists: Long,
                          entryKills: Long,
                          damageDone: Long,
                          mvps: Long,
                          rating: Float,
                          plants: Long,
                          defuses: Long,
                          clutchesWon: Long,
                          created: DateTime,
                          changed: DateTime
                          )

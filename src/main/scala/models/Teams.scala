package models

import play.api.libs.json.Json

case class Teams(team1: List[AdditionalPlayerStats], team2: List[AdditionalPlayerStats])

case class AdditionalPlayerStats(steamId: String, steamId64bit: String, name: String, kills: Int, assists: Int, deaths: Int, score: Int, rating: Double)

object AdditionalPlayerStats {
  implicit val additionalPlayerStatsFormat = Json.format[AdditionalPlayerStats]
}

object Teams {
  implicit val overAllDemoStatsFormat = Json.format[Teams]
}
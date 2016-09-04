package models

import play.api.libs.json.Json
import models.PlayerRoundStats._

case class RoundStat(
                      roundNumber: Int,
                      playerStats: List[PlayerRoundStats],
                      winningSide: String,
                      winningTeam: Int,
                      reason: String,
                      highlight: Boolean,
                      CTEquipmentValue: Int,
                      TEquipmentValue: Int,
                      CTStartMoney: Int,
                      TStartMoney: Int
                      )

object RoundStat {
  implicit val roudStatFormat = Json.format[RoundStat]
}
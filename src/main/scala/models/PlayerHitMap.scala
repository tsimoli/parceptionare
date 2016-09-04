package models

import play.api.libs.json.Json


case class PlayerHitMap(
                         steamId: String,
                         name: String,
                         totalDamageDone: Long = 0,
                         sum: Int = 0,
                         generic: Int = 0,
                         head: Int = 0,
                         chest: Int = 0,
                         stomach: Int = 0,
                         leftArm: Int = 0,
                         rightArm: Int = 0,
                         leftLeg: Int = 0,
                         rightLeg: Int = 0,
                         gear: Int = 0)

object PlayerHitMap {
  implicit val playerHitMapFormat = Json.format[PlayerHitMap]
}

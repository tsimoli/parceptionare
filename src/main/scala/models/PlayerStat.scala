package models

import models.ClutchType.ClutchType
import models.MvpType.MvpType
import play.api.libs.json.{JsValue, JsResult, JsSuccess, Json}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

object ClutchType extends Enumeration {
  type ClutchType = Value
  val oneVsOne = Value("1vs1")
  val oneVsTwo = Value("1vs2")
  val oneVsThree = Value("1vs3")
  val oneVsFour = Value("1vs4")
  val oneVsFive = Value("1vs5")

  implicit object ClutchTypeFormat extends Format[ClutchType] {
    def reads(json: JsValue): JsResult[ClutchType] = JsSuccess(ClutchType.withName(json.as[String]))

    def writes(clutchType: ClutchType): JsString = JsString(clutchType.toString)
  }

}

object MvpType extends Enumeration {
  type MvpType = Value
  val mostKills = Value("mostKills")
  val bombPlanted = Value("bombPlanted")
  val bombDefused = Value("bombDefused")

  implicit object MvpTypeFormat extends Format[MvpType] {
    def reads(json: JsValue): JsResult[MvpType] = JsSuccess(MvpType.withName(json.as[String]))

    def writes(mvpType: MvpType): JsString = JsString(mvpType.toString)
  }
}

case class PlayerRoundStats(roundNum: Int,
                            steamId: String,
                            steamId64bit: String,
                            name: String,
                            team: String,
                            entryKill: Boolean,
                            kills: List[Kill],
                            assists: Int,
                            died: Boolean,
                            clutch: Option[ClutchType],
                            mvpType: Option[MvpType],
                            equipmentValue: Int,
                            startMoney: Int,
                            bombPlanted: Boolean,
                            bombDefused: Boolean,
                            utilityBought: Int,
                            utilityUsed: Int,
                            entryDuel: Option[Boolean],
                            possibleClutch: Option[ClutchType]
                           )

case class Kill(killed: String, killerPosition: PlayerPosition, deadPosition: PlayerPosition, weaponUsed: String, weaponType: String, headShot: Boolean, place: String, penetratedObjects: Int)

case class PlayerPosition(x: Float, y: Float, z: Float)

object PlayerRoundStats {
  implicit val playerPositionFormat = Json.format[PlayerPosition]
  implicit val killFormat = Json.format[Kill]
  implicit val playerStatFormat = Json.format[PlayerRoundStats]
}
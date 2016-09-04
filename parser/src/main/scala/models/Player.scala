package models

import parsers.main.Enums.Equipment

import scala.collection.mutable.HashMap

class Player(
              var name: String,
              var steamID: String,
              var steamId64bit: String,
              var userId: Int,
              var positionX: Float,
              var positionY: Float,
              var positionZ: Float,
              var entityId: Int,
              var hp: Int,
              var armor: Int,
              var viewDirectionX: Float,
              var viewDirectionY: Float,
              var money: Int,
              var activeWeaponId: Int,
              var rawWeapons: scala.collection.mutable.HashMap[Int, Equipment],
              var weapons: List[Int],
              var hasDefuseKit: Boolean,
              var hasHelmet: Boolean,
              var teamId: Int,
              var ammoLeft: Array[Int] = new Array(32),
              var additionalInformation: Option[AdditionalInformation],
              var currentEquipmentValue: Int,
              var roundStartEquipmentValue: Int,
              var freezeTimeEquipmentValue: Int,
              var lastPlace: String,
              var originalEntityId: Int,
              var team: String,
              var disconnected: Boolean) {

  def activeWeapon() = {
    if (activeWeaponId == ((1 << 11) - 1)) None
    else {
      rawWeapons.get(activeWeaponId)
    }
  }

  def isAlive = hp > 0 && !disconnected
}

case class AdditionalInformation(var clanTag: String,
                                 var score: Int,
                                 var kills: Int,
                                 var deaths: Int,
                                 var assists: Int,
                                 var mvps: Int,
                                 var totalCashSpent: Int,
                                 var entryKills: Long,
                                 var damageDone: Long,
                                 var plants: Long,
                                 var defuses: Long,
                                 var clutchesWon: Long,
                                 var clutchesLost: Long,
                                 var ping: Int)

object Player {
  def createDefaultPlayer(entityId: Int) = {
    new Player(
      "",
      "",
      "",
      0,
      0.0.toFloat,
      0.0.toFloat,
      0.0.toFloat,
      entityId,
      0,
      0,
      0.0.toFloat,
      0.0.toFloat,
      0,
      -1,
      HashMap[Int, Equipment](),
      List[Int](),
      false,
      false,
      -1,
      new Array[Int](32),
      Some(AdditionalInformation("", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
      0,
      0,
      0,
      "",
      entityId,
      "",
      false)
  }

  def createPlayerOnConnect(userId: Int, name: String, steamId: String, entityId: Int) = {
    new Player(
      name,
      steamId,
      transformSteamIdTo64bit(steamId).toString,
      userId,
      0.0.toFloat,
      0.0.toFloat,
      0.0.toFloat,
      entityId,
      0,
      0,
      0.0.toFloat,
      0.0.toFloat,
      0,
      -1,
      HashMap[Int, Equipment](),
      List[Int](),
      false,
      false,
      -1,
      new Array[Int](32),
      Some(AdditionalInformation("", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
      0,
      0,
      0,
      "",
      entityId,
      "",
      false)
  }

  private def transformSteamIdTo64bit(steamId: String): Long = {
    val authServerAndId = steamId.splitAt(8)
    val steamIdArray = authServerAndId._2.split(":")
    if (steamIdArray.size == 2) {
      (steamIdArray(1).toLong * 2) + (steamIdArray(0).toLong + 76561197960265728L)
    }
    else 0
  }
}
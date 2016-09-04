package models

import model.RankUpdate
import parsers.main.Enums.Equipment

object Events {
  case class PlayerDeathEvent(playerDeath: PlayerDeath)
  case class RoundEndEvent(winnerTeam: Option[Int], reason: Option[Int], message: String)
  case class RoundOfficiallyEndEvent()
  case class RoundStartEvent()
  case class FreezeTimeEndEvent()
  case class RoundMVPEvent(player: Option[Player], reason: Option[Int])
  case class Position(x: Float, y: Float, z: Float)
  case class NadeEvent(flashedPlayers: Option[List[Player]], position: Position, thrownBy: Option[Player])
  case class BombEvent(player: Option[Player], site: String)
  case class WeaponFireEvent(shooter: Option[Player], weapon: Equipment)
  case class BombDefuseEvent(player: Option[Player], hasDefuseKit: Boolean)
  case class BeginBombPlantEvent()
  case class BombPlantedEvent()
  case class AbortBombPlantEvent()
  case class BombDefusedEvent()
  case class BombExplodedEvent()
  case class PlayerDeath(dead: Option[Player], killer: Option[Player], assister: Option[Player], headShot: Boolean, weaponUsed: Equipment, penetratedObjects: Int)
  case class PlayerHurtEvent(hurtPlayer: Option[Player], attacker: Option[Player], health: Int, armor: Int, healthDamage: Int, armorDamage: Int, hitGroup: HitGroup.Value, weapon: Option[Equipment])
  case class HeaderParsedEvent(header: Header)
  case class RankUpdateEvent(rankUpdates: List[RankUpdate])
}
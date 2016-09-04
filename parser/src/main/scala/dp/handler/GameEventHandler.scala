package dp.handler

import dp.{Descriptor, GameEvent}
import models.Events._
import models.{HitGroup, Player}
import parsers.DemoParser
import parsers.main.Enums.{Equipment, EquipmentElement}

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

object GameEventHandler {
  def handleGameEventList(gel: ListBuffer[Descriptor], parser: DemoParser) = {
    gel.foreach { d =>
      parser.GEH_Descriptors += d.eventId -> d
    }
  }

  def apply(rawEvent: GameEvent, parser: DemoParser) = {
    breakable {
      val blindPlayers = parser.GEH_BlindPlayers

      val eventDescriptor = parser.GEH_Descriptors(rawEvent.eventId)
      var data = scala.collection.mutable.Map[String, Any]()

      if (parser.GEH_Descriptors == null)
        break()

      if (eventDescriptor.name == "round_start") {
        parser.raiseRoundStart(RoundStartEvent())
      }

      if (eventDescriptor.name == "round_end") {
        data = mapData(eventDescriptor, rawEvent)
        val winnerTeam = Option(data("winner").asInstanceOf[Int])
        val winner = winnerTeam.getOrElse(0)
        val reason = Option(data("reason").asInstanceOf[Int])
        val message = Option(data("message").asInstanceOf[String]).getOrElse("")
        parser.raiseRoundEndEvent(RoundEndEvent(Some(winner), reason, message))
      }

      if (eventDescriptor.name == "round_officially_ended") {
        parser.raiseRoundOfficiallyEnd
      }

      if (eventDescriptor.name == "begin_new_match") {
        println("========================================")
        parser.raiseBeginNewMath
      }

      if (eventDescriptor.name == "round_freeze_end") {
        parser.raiseFreezeTime(FreezeTimeEndEvent())
      }

      eventDescriptor.name match {
        case "round_mvp" =>
          data = mapData(eventDescriptor, rawEvent)
          val mvpPlayer = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          val reason = Option(data("reason").asInstanceOf[Int])
          parser.raiseRoundMVPEvent(RoundMVPEvent(mvpPlayer, reason))
        case "weapon_fire" =>
          data = mapData(eventDescriptor, rawEvent)
          val shooter = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          val weapon = Equipment()
          weapon.originalString = data("weapon").asInstanceOf[String]
          weapon.weapon = EquipmentElement.mapEquipment(weapon.originalString)
          parser.raiseWeaponFireEvent(WeaponFireEvent(shooter, weapon))
        case "player_hurt" =>
          data = mapData(eventDescriptor, rawEvent)
          val hurtPlayer = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          val attacker = parser.players(Option(data("attacker").asInstanceOf[Int]).getOrElse(-1))
          val health = data("health").asInstanceOf[Int]
          val armor = data("armor").asInstanceOf[Int]
          val healthDamage = data("dmg_health").asInstanceOf[Int]
          val armorDamage = data("dmg_armor").asInstanceOf[Int]
          val hitGroup = data("hitgroup").asInstanceOf[Int]
          val weapon = if (!data("weapon").asInstanceOf[String].isEmpty) {
            val equipment = Equipment()
            equipment.originalString = data("weapon").asInstanceOf[String]
            equipment.weapon = EquipmentElement.mapEquipment(equipment.originalString)
            Some(equipment)
          } else None

          parser.raisePlayerHurtEvent(PlayerHurtEvent(hurtPlayer, attacker, health, armor, healthDamage, armorDamage, HitGroup(hitGroup), weapon))
        case "player_death" =>
          data = mapData(eventDescriptor, rawEvent)
          val dead = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          val killer = parser.players(Option(data("attacker").asInstanceOf[Int]).getOrElse(-1))
          val assister = parser.players.flatten.find { p => p.userId == data("assister").asInstanceOf[Int] }
          val headShot = data("headshot").asInstanceOf[Boolean]
          val weaponUsed = new Equipment()
          
          weaponUsed.originalString = data("weapon").asInstanceOf[String]
          weaponUsed.skinID = data("weapon_itemid").asInstanceOf[String]
          weaponUsed.weapon = EquipmentElement.mapEquipment(weaponUsed.originalString)
          val penetratedObjects = data("penetrated").asInstanceOf[Int]

          val playerDeath = PlayerDeath(dead, killer, assister, headShot, weaponUsed, penetratedObjects)
          parser.raisePlayerDeathEvent(playerDeath)
        case "player_blind" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1)).foreach { player =>
            parser.GEH_BlindPlayers = player :: parser.GEH_BlindPlayers
          }
        case "flashbang_detonate" =>
          data = mapData(eventDescriptor, rawEvent)
          var args = createNadeEvent(data, parser)
          args = args.copy(flashedPlayers = Some(parser.GEH_BlindPlayers))
          parser.GEH_BlindPlayers = List()
          parser.raiseFlashDetonateEvent(args)
        case "hegrenade_detonate" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseHEDetonateEvent(createNadeEvent(data, parser))
        case "decoy_started" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseDecoyStartedEvent(createNadeEvent(data, parser))
        case "decoy_detonate" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseDecoyDetonateEvent(createNadeEvent(data, parser))
        case "smokegrenade_detonate" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseSmokeDetonateEvent(createNadeEvent(data, parser))
        case "smokegrenade_expired" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseSmokeExpiredEvent(createNadeEvent(data, parser))
        case "inferno_startburn" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseInfernoStartEvent(createNadeEvent(data, parser))
        case "inferno_expire" =>
          data = mapData(eventDescriptor, rawEvent)
          parser.raiseInfernoExpireEvent(createNadeEvent(data, parser))
        case "player_connect" =>
          data = mapData(eventDescriptor, rawEvent)
          val userId = data("userid").asInstanceOf[Int]
          val name = data("name").asInstanceOf[String]
          val index = data("index").asInstanceOf[Int]
          val steamId = data("networkid").asInstanceOf[String]
          val player = Some(Player.createPlayerOnConnect(userId, name, steamId, index + 1))
          if (steamId != "BOT") {
            var found = false
            parser.players.foreach { player =>
              if(player.nonEmpty && player.get.steamID == steamId) {
                if(player.get.userId != userId)
                  parser.players.update(player.get.userId, None)
                var updatedPlayer = player.get
                updatedPlayer.userId = userId
                updatedPlayer.name = name
                updatedPlayer.steamID = steamId
                updatedPlayer.entityId = index + 1
                updatedPlayer.disconnected = false
                parser.players.update(userId, Some(updatedPlayer))
                found = true
              }
            }

            if(!found) {
              parser.players(userId) = player
            }
          }
        case "player_disconnect" =>
          data = mapData(eventDescriptor, rawEvent)
          val disconnectedId = Option(data("userid").asInstanceOf[Int]).getOrElse(-1)
          if (disconnectedId > 0) {
            parser.players(disconnectedId).find(player => player.userId == disconnectedId).map { found =>
              parser.players.updated(disconnectedId, Some(found.disconnected = true))
            }
          }
        case "bomb_beginplant" | "bomb_abortplant" | "bomb_planted" | "bomb_defused" | "bomb_exploded " =>
          data = mapData(eventDescriptor, rawEvent)
          val player = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          val site = data("site").asInstanceOf[Int]
          var bombEvent = new BombEvent(player, "")
          if (parser.bombSiteAIndex == site) bombEvent = bombEvent.copy(site = "A")
          else if (parser.bombSiteBIndex == site) bombEvent = bombEvent.copy(site = "B")
          else {
            parser.triggers.find(trigger => trigger.index == site).foreach { trigger =>
              if (trigger.contains(parser.bombSiteACenter)) {
                bombEvent = bombEvent.copy(site = "A")
                parser.bombSiteAIndex = site
              } else if (trigger.contains(parser.bombSiteBCenter)) {
                bombEvent = bombEvent.copy(site = "B")
                parser.bombSiteBIndex = site
              } else {
                // I'm done
              }
            }

          }
          eventDescriptor.name match {
            case "bomb_beginplant" => parser.raiseBeginBombPlantEvent(bombEvent)
            case "bomb_abortplant" => parser.raiseAbortBombPlantEvent(bombEvent)
            case "bomb_planted" => parser.raiseBombPlantedEvent(bombEvent)
            case "bomb_defused" => parser.raiseBombDefusedEvent(bombEvent)
            case "bomb_exploded" => parser.raiseBombExplodedEvent(bombEvent)
          }
        case "bomb_begindefuse" =>
          data = mapData(eventDescriptor, rawEvent)
          val player = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          player.foreach { p =>
            val hasKit = p.hasDefuseKit
            parser.raiseBombBeginDefuseEvent(BombDefuseEvent(player, hasKit))
          }
        case "bomb_abortdefuse" =>
          data = mapData(eventDescriptor, rawEvent)
          val player = parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1))
          player.foreach { p =>
            val hasKit = p.hasDefuseKit
            BombDefuseEvent(player, hasKit)
            parser.raiseBombAbortDefuseEvent(BombDefuseEvent(player, hasKit))
          }
        case _ =>
      }
    }
  }

  def createNadeEvent(data: scala.collection.mutable.Map[String, Any], parser: DemoParser) = {
    val position = Position(data("x").asInstanceOf[Float], data("y").asInstanceOf[Float], data("z").asInstanceOf[Float])
    val thrownBy = if (data.contains("userid")) parser.players(Option(data("userid").asInstanceOf[Int]).getOrElse(-1)) else None
    NadeEvent(None, position, thrownBy)
  }

  def mapData(eventDesciptor: Descriptor, rawEvent: GameEvent) = {
    val data = scala.collection.mutable.Map[String, Any]()
    for (i <- eventDesciptor.keys.indices)
      data += (eventDesciptor.keys(i).name -> rawEvent.keys(i))

    data
  }

}
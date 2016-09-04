package parsers

import com.google.protobuf.CodedInputStream
import dp.{CreateStringTable, Descriptor}
import dt.DataTableParser
import models.Events._
import models._
import org.joda.time.DateTime
import parsers.main.Enums.Weapon.Weapon
import parsers.main.Enums.{Equipment, EquipmentElement, Weapon}
import play.api.Logger
import play.api.libs.json.Json
import rx.lang.scala.Subject
import stream.BitStream
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DemoParser(input: CodedInputStream) {
  val dataTableParser = new DataTableParser()
  val stringTableParser = new StringTableparser
  var equipmentMapping: mutable.Map[ServerClass, Weapon] = mutable.Map.empty
  val instanceBaseline = mutable.Map[Int, Array[Byte]]()
  val preprocessedBaselines = mutable.Map[Int, Array[Object]]()
  val GEH_Descriptors = collection.mutable.Map[Int, Descriptor]()
  var GEH_BlindPlayers = List[Player]()
  var entities = new Array[Entity](2048)
  var players = new Array[Option[Player]](128)
  val stringTables = ListBuffer[CreateStringTable]()
  var currentTick = 0
  var modelprecache = ListBuffer[String]()
  var CTScore = 0
  var TScore = 0
  var CTId = -1
  var TId = -1
  var weapons = scala.collection.mutable.HashMap[Int, Equipment]()
  var matchStarted = false
  var bombSiteAIndex = -1
  var bombSiteBIndex = -1
  var bombSiteACenter = models.Vector(0, 0, 0)
  var bombSiteBCenter = models.Vector(0, 0, 0)
  var triggers = List[BoundingBoxInformation]()

  def parseDemo() = {
    val header = HeaderParser.parseHeader(input)
    if (header.fileStamp == "HL2DEMO" || header.protocol == 4) {
      raiseHeaderParsed(header)
      players = Array.fill(128)(None)
      println(header)
      while (parseTick()) {
      }
      true
    } else {
      Logger.error("Failed to parse demo header: " + header)
      false
    }
  }

  private def parseTick(): Boolean = {

    val demoCommand = DemoCommand(input.readRawByte())
    input.readRawLittleEndian32() // SeqNrIn
    input.readRawByte() // SeqNrOut
    currentTick += 1

    demoCommand match {
      case DemoCommand.Synctick =>
        true
      case DemoCommand.Stop =>
        Logger.info("Finished demo parsing")
        raiseMatchOfficiallyEnded()
        false
      case DemoCommand.ConsoleCommand =>
        input.readRawBytes(input.readRawLittleEndian32())
        false
      case DemoCommand.DataTables =>
        val length = input.readRawLittleEndian32()
        dataTableParser.parsePacket(new BitStream(input.readRawBytes(length)))
        mapEquipment()
        bindEntities()
        true
      case DemoCommand.StringTables =>
        stringTableParser.parsePacket(new BitStream(input.readRawBytes(input.readRawLittleEndian32())), this)
        true
      case DemoCommand.UserCommand =>
        input.readRawBytes(input.readRawLittleEndian32())
        true
      case DemoCommand.Signon | DemoCommand.Packet =>
        parseDemoPacket()
        true
      case _ => Logger.error("Demo command not recognized"); false
    }
  }

  private def parseDemoPacket() {
    CommandInfo.parse(input)
    input.readRawLittleEndian32()
    input.readRawLittleEndian32()
    val length = input.readRawLittleEndian32() // next chunk length

    DemoPacketParser.parsePacket(CodedInputStream.newInstance(input.readRawBytes(length)), this)
  }

  def mapEquipment() {
    dataTableParser.serverClasses.foreach { serverClass =>
      if (serverClass.baseClasses.size > 6 && serverClass.baseClasses(6).name == "CWeaponCSBase") {
        if (serverClass.baseClasses.size > 7) {
          if (serverClass.baseClasses(7).name == "CWeaponCSBaseGun") {
            equipmentMapping = equipmentMapping += serverClass -> EquipmentElement.mapEquipment(serverClass.dtName.substring(9).toLowerCase)
          } else if (serverClass.baseClasses(7).name == "CBaseCSGrenade") {
            equipmentMapping = equipmentMapping += serverClass -> EquipmentElement.mapEquipment(serverClass.dtName.substring(3).toLowerCase)
          }
        } else if (serverClass.name == "CC4") {
          equipmentMapping = equipmentMapping += serverClass -> Weapon.Bomb
        } else if (serverClass.name == "CKnife" || (serverClass.baseClasses.size > 6 && serverClass.baseClasses(6).name == "CKnife")) {
          equipmentMapping = equipmentMapping += serverClass -> Weapon.Knife
        } else if (serverClass.name == "CWeaponNOVA" || serverClass.name == "CWeaponSawedoff" || serverClass.name == "CWeaponXM1014") {
          equipmentMapping = equipmentMapping += serverClass -> EquipmentElement.mapEquipment(serverClass.name.substring(7).toLowerCase)
        }
      }
    }
  }

  def bindEntities() = {
    handleTeamScores()
    handleBombSites()
    handlePlayers()
    handleWeapons()
  }

  def handleTeamScores() = {
    dataTableParser.findByName("CCSTeam").newEntitySubject.subscribe(e => {
      var team = ""
      var score = 0
      var teamId = -1
      e.entity.findProperty("m_scoreTotal").map { prop =>
        prop.intSubject.subscribe {
          i => score = i
        }
      }

      e.entity.findProperty("m_szTeamname").map { prop =>
        prop.stringSubject.subscribe { s =>
          team = s
          if (s == "CT") {
            CTScore = score
            e.entity.findProperty("m_scoreTotal").map { prop =>
              prop.intSubject.subscribe { i =>
                CTScore = i
              }
            }
            if (teamId != -1) {
              CTId = teamId
              players.foreach(playerOption => playerOption.foreach { p => if (p.teamId == teamId) p.team = "CT" })
            }
          } else if (s == "TERRORIST") {
            TScore = score
            e.entity.findProperty("m_scoreTotal").map { prop =>
              prop.intSubject.subscribe { i =>
                TScore = i
              }
            }
            if (teamId != -1) {
              TId = teamId
              players.foreach(playerOption => playerOption.foreach { p => if (p.teamId == teamId) p.team = "TERRORIST" })
            }
          }
        }
      }
    })
  }

  def handlePlayers() = {
    dataTableParser.findByName("CCSPlayer").newEntitySubject.subscribe(e => handlePlayer(e.entity))
    dataTableParser.findByName("CCSPlayerResource").newEntitySubject.subscribe { e =>
      for (i <- 0 to 64) {
        val entityString = i.toString.reverse.padTo(3, "0").reverse.mkString("")
        e.entity.findProperty("m_szClan." + entityString).map { prop =>
          prop.stringSubject.subscribe { clanTag =>
            players.flatten.toList.find(_.entityId == i).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.clanTag = clanTag
              }
            }
          }
        }
        e.entity.findProperty("m_iPing." + entityString).map { prop =>
          prop.intSubject.subscribe { ping =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.ping = ping
              }
            }
          }
        }
        e.entity.findProperty("m_iScore." + entityString).map { prop =>
          prop.intSubject.subscribe { score =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.score = score
              }
            }
          }
        }
        e.entity.findProperty("m_iKills." + entityString).map { prop =>
          prop.intSubject.subscribe { kills =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.kills = kills
              }
            }
          }
        }

        e.entity.findProperty("m_iAssists." + entityString).map { prop =>
          prop.intSubject.subscribe { assists =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.assists = assists
              }
            }
          }
        }

        e.entity.findProperty("m_iDeaths." + entityString).map { prop =>
          prop.intSubject.subscribe { deaths =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.deaths = deaths
              }
            }
          }
        }
        e.entity.findProperty("m_iMVPs." + entityString).map { prop =>
          prop.intSubject.subscribe { mvps =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.mvps = mvps
              }
            }
          }
        }
        e.entity.findProperty("m_iTotalCashSpent." + entityString).map { prop =>
          prop.intSubject.subscribe { totalCashSpent =>
            players.flatten.toList.find(p => p.entityId == i && !p.disconnected).foreach { foundPlayer =>
              foundPlayer.additionalInformation.foreach { additionalInformation =>
                additionalInformation.totalCashSpent = totalCashSpent
              }
            }
          }
        }
      }
    }
  }

  def handlePlayer(entity: Entity) = {
    val player = players.flatten.find { p => p.entityId == entity.id }.getOrElse {
      Player.createDefaultPlayer(entity.id)
    }

    player.entityId = entity.id
    entity.findProperty("cslocaldata.m_vecOrigin").map { prop =>
      prop.vectorSubject.subscribe { vector =>
        if (!player.disconnected) {
          player.positionX = vector.x
          player.positionY = vector.y
          player.positionZ = vector.z
        }
      }
    }

    entity.findProperty("cslocaldata.m_vecOrigin[2]").map { prop =>
      prop.floatSubject.subscribe(vectorZ => player.positionZ = vectorZ)
    }

    entity.findProperty("m_iTeamNum").map { prop =>
      prop.intSubject.subscribe(teamId => {
        if (!player.disconnected) {
          player.teamId = teamId
          if (teamId == 3) player.team = "CT"
          else if (teamId == 2) player.team = "TERRORIST"
          else player.team = "SPECTATOR"
        }
      })
    }
    entity.findProperty("m_iHealth").map { prop =>
      prop.intSubject.subscribe(health => {
        if (!player.disconnected) {
          player.hp = health
        }
      })
    }
    entity.findProperty("m_ArmorValue").map { prop =>
      prop.intSubject.subscribe(armor => if (!player.disconnected) player.armor = armor)
    }
    entity.findProperty("m_bHasDefuser").map { prop =>
      prop.intSubject.subscribe(defuser => if (!player.disconnected) {
        if (defuser == 1)
          player.hasDefuseKit = true
        else
          player.hasDefuseKit = false
      })
    }
    entity.findProperty("m_bHasHelmet").map { prop =>
      prop.intSubject.subscribe(helmet => if (!player.disconnected) {
        if (helmet == 1)
          player.hasHelmet = true
        else player.hasHelmet = false
      }
      )
    }
    entity.findProperty("m_iAccount").map { prop =>
      prop.intSubject.subscribe(money => if (!player.disconnected) player.money = money)
    }
    entity.findProperty("m_angEyeAngles[1]").map { prop =>
      prop.floatSubject.subscribe(viewDirectionX => if (!player.disconnected) player.viewDirectionX = viewDirectionX)
    }
    entity.findProperty("m_angEyeAngles[0]").map { prop =>
      prop.floatSubject.subscribe(viewDirectionY => if (!player.disconnected) player.viewDirectionY = viewDirectionY)
    }
    entity.findProperty("m_unCurrentEquipmentValue").map { prop =>
      prop.intSubject.subscribe(currentEquipmentValue => if (!player.disconnected) player.currentEquipmentValue = currentEquipmentValue)
    }
    entity.findProperty("m_unRoundStartEquipmentValue").map { prop =>
      prop.intSubject.subscribe(roundStartEquipmentValue => if (!player.disconnected) player.roundStartEquipmentValue = roundStartEquipmentValue)
    }
    entity.findProperty("m_unFreezetimeEndEquipmentValue").map { prop =>
      prop.intSubject.subscribe(freezeTimeEquipmentValue => if (!player.disconnected) player.freezeTimeEquipmentValue = freezeTimeEquipmentValue)
    }

    entity.findProperty("m_szLastPlaceName").map { prop =>
      prop.stringSubject.subscribe(lastPlace =>if (!player.disconnected)  player.lastPlace = lastPlace)
    }

    entity.props.filter(entry => entry.flattenedPropEntry.propertyName.startsWith("m_hMyWeapons.")).foreach { entry =>
      entry.intSubject.subscribe { value =>
        val index = value & ((1 << 11) - 1)
        val slot = entry.flattenedPropEntry.propertyName.split("m_hMyWeapons.")(1)
        val slotNumber = if (slot.startsWith("0")) slot.tail.toInt else slot.toInt
        if (index != ((1 << 11) - 1)) {
          weapons.get(index).foreach { weapon =>
            if (!player.disconnected)
              player.rawWeapons = player.rawWeapons += slotNumber -> weapon
          }
        } else {
          if (!player.disconnected)
            player.rawWeapons.remove(slotNumber)
        }
      }
    }

    entity.findProperty("m_hActiveWeapon").map { prop =>
      prop.intSubject.subscribe(activeWeaponId => if (!player.disconnected) player.activeWeaponId = activeWeaponId & ((1 << 11) - 1))
    }
  }

  def handleWeapons() = {
    for (i <- 0 until 2048) {
      weapons(i) = new Equipment()
    }
    dataTableParser.serverClasses.filter(serverClass => serverClass.baseClasses.exists { baseClass => baseClass.name == "CWeaponCSBase" }).foreach { serverClass =>
      serverClass.newEntitySubject.subscribe(e => handleWeapon(e))
    }
  }

  def handleWeapon(eventArgs: NewEntityArgs) = {
    val entity = eventArgs.entity
    val equipment = weapons(entity.id)
    equipment.entityID = entity.id
    entity.findProperty("m_iClip1").map { prop =>
      prop.intSubject.subscribe(ammoInMagazine => equipment.ammoInMagazine = ammoInMagazine)
    }
    entity.findProperty("LocalWeaponData.m_iPrimaryAmmoType").map { prop =>
      prop.intSubject.subscribe(ammoType => equipment.ammoType = ammoType)
    }
    equipmentMapping.get(entity.serverClass).foreach { weapon =>
      equipment.weapon = weapon
      if (weapon == Weapon.P2000) {
        entity.findProperty("m_nModelIndex").map { prop =>
          prop.intSubject.subscribe { model =>
            val precacheValue = modelprecache(model)
            equipment.originalString = precacheValue
            if (equipment.originalString.contains("_pist_223")) {
              equipment.weapon = Weapon.USP
            } else if (equipment.originalString.contains("_pist_hkp2000")) {
              equipment.weapon = Weapon.P2000
            }
          }
        }
      }
      if (weapon == Weapon.M4A4) {
        entity.findProperty("m_nModelIndex").map { prop =>
          prop.intSubject.subscribe { model =>
            val precacheValue = modelprecache(model)
            equipment.originalString = precacheValue
            if (precacheValue.contains("_rif_m4a1_s")) {
              equipment.weapon = Weapon.M4A1
            } else if (precacheValue.contains("_rif_m4a1")) {
              equipment.weapon = Weapon.M4A4
            }
          }
        }
      }
      if (weapon == Weapon.P250) {
        entity.findProperty("m_nModelIndex").map { prop =>
          prop.intSubject.subscribe { model =>
            val precacheValue = modelprecache(model)
            equipment.originalString = precacheValue
            if (equipment.originalString.contains("_pist_cz_75")) {
              equipment.weapon = Weapon.CZ
            } else if (equipment.originalString.contains("_pist_p250")) {
              equipment.weapon = Weapon.P250
            }
          }
        }
      }
      weapons += entity.id -> equipment
      players.foreach { player =>
        player.foreach { p =>
          p.rawWeapons.foreach { weapon =>
            if (weapon._2.entityID == entity.id) {
              if (!p.disconnected)
                p.rawWeapons = p.rawWeapons += weapon._1 -> equipment
            }
          }
        }
      }
    }
  }

  def handleBombSites() = {
    dataTableParser.findByName("CCSPlayerResource").newEntitySubject.subscribe { e =>
      e.entity.findProperty("m_bombsiteCenterA").map { prop =>
        prop.vectorSubject.subscribe { vector =>
          bombSiteACenter = vector
        }
      }
      e.entity.findProperty("m_bombsiteCenterB").map { prop =>
        prop.vectorSubject.subscribe { vector =>
          bombSiteBCenter = vector
        }
      }
    }

    dataTableParser.findByName("CBaseTrigger").newEntitySubject.subscribe { e =>
      val trigger = new BoundingBoxInformation
      trigger.index = e.entity.id
      triggers = trigger :: triggers
      e.entity.findProperty("m_Collision.m_vecMins").map { prop =>
        prop.vectorSubject.subscribe { vector =>
          val triggerIndex = triggers.indexWhere(_.index == e.entity.id)
          if (triggerIndex != -1)
            triggers.updated(triggerIndex, triggers(triggerIndex).min = vector)
        }
      }
      e.entity.findProperty("m_Collision.m_vecMaxs").map { prop =>
        prop.vectorSubject.subscribe { vector =>
          val triggerIndex = triggers.indexWhere(_.index == e.entity.id)
          if (triggerIndex != -1)
            triggers.updated(triggerIndex, triggers(triggerIndex).max = vector)
        }
      }
    }
  }

  val playerDeathSubject = Subject[PlayerDeath]()
  val roundEndSubject = Subject[RoundEndEvent]()
  val roundOfficiallyEnd = Subject[RoundOfficiallyEndEvent]
  val roundStartSubject = Subject[RoundStartEvent]()
  val roundMVPSubject = Subject[RoundMVPEvent]()
  val flashDetonateSubject = Subject[NadeEvent]()
  val HEDetonateSubject = Subject[NadeEvent]()
  val decoyStartedSubject = Subject[NadeEvent]()
  val decoyDetonateSubject = Subject[NadeEvent]()
  val smokeDetonateSubject = Subject[NadeEvent]()
  val smokeExpiredSubject = Subject[NadeEvent]()
  val infernoStartedSubject = Subject[NadeEvent]()
  val infernoExpiredSubject = Subject[NadeEvent]()
  val weaponFireSubject = Subject[WeaponFireEvent]()
  val beginBombDefuseSubject = Subject[BombDefuseEvent]()
  val abortBombDefuseSubject = Subject[BombDefuseEvent]()
  val bombDefusedSubject = Subject[BombEvent]()
  val bombPlantedSubject = Subject[BombEvent]()
  val bombExplodedSubject = Subject[BombEvent]()
  val matchEndedSubject = Subject[Boolean]()
  val beginNewMatchSubject = Subject[Boolean]()
  val freezeTimeEndSubject = Subject[FreezeTimeEndEvent]()
  val playerHurtSubject = Subject[PlayerHurtEvent]()
  val rankUpdateSubject = Subject[RankUpdateEvent]()
  val headerParsed = Subject[HeaderParsedEvent]()

  def raiseHeaderParsed(header: Header) = {
    headerParsed.onNext(HeaderParsedEvent(header))
  }

  def raisePlayerDeathEvent(playerDeath: PlayerDeath) {
    playerDeathSubject.onNext(playerDeath)
  }

  def raiseRoundStart(roundStartEvent: RoundStartEvent) = {
    roundStartSubject.onNext(roundStartEvent)
  }

  def raiseRoundEndEvent(roundEndEvent: RoundEndEvent) = {
    roundEndSubject.onNext(roundEndEvent)
  }

  def raiseRoundMVPEvent(roundMVPEvent: RoundMVPEvent) = {
    roundMVPSubject.onNext(roundMVPEvent)
  }

  def raiseFreezeTime(freezeTimeEndEvent: FreezeTimeEndEvent) = {
    freezeTimeEndSubject.onNext(freezeTimeEndEvent)
  }

  def raiseRoundOfficiallyEnd() = {
    roundOfficiallyEnd.onNext(RoundOfficiallyEndEvent())
  }

  def raiseRankUpdate(rankUpdateEvent: RankUpdateEvent) = {
    rankUpdateSubject.onNext(rankUpdateEvent)
  }

  def raiseFlashDetonateEvent(nadeEvent: NadeEvent) = {
    flashDetonateSubject.onNext(nadeEvent)
  }

  def raiseHEDetonateEvent(nadeEvent: NadeEvent) = {
    HEDetonateSubject.onNext(nadeEvent)
  }

  def raiseDecoyStartedEvent(nadeEvent: NadeEvent) = {
    decoyStartedSubject.onNext(nadeEvent)
  }

  def raiseDecoyDetonateEvent(nadeEvent: NadeEvent) = {

  }

  def raiseSmokeDetonateEvent(nadeEvent: NadeEvent) = {
    smokeDetonateSubject.onNext(nadeEvent)
  }

  def raiseSmokeExpiredEvent(nadeEvent: NadeEvent) = {

  }

  def raiseInfernoStartEvent(nadeEvent: NadeEvent) = {
    infernoStartedSubject.onNext(nadeEvent)
  }

  def raiseInfernoExpireEvent(nadeEvent: NadeEvent) = {

  }

  def raiseBombEvent(bombEvent: BombEvent) = {

  }

  def raiseWeaponFireEvent(weaponFireEvent: WeaponFireEvent) = {

  }

  def raiseBombBeginDefuseEvent(bombDefuseEvent: BombDefuseEvent) = {

  }

  def raiseBombAbortDefuseEvent(bombDefuseEvent: BombDefuseEvent) = {

  }

  def raiseBeginBombPlantEvent(bombDefuseEvent: BombEvent) = {

  }

  def raiseAbortBombPlantEvent(abortBombPlantEvent: BombEvent) = {

  }

  def raiseBombPlantedEvent(bombPlantedEvent: BombEvent) = {
    bombPlantedSubject.onNext(bombPlantedEvent)
  }

  def raiseBombDefusedEvent(bombDefusedEvent: BombEvent) = {
    bombDefusedSubject.onNext(bombDefusedEvent)
  }

  def raiseBombExplodedEvent(bombExplodedEvent: BombEvent) = {

  }

  def raiseMatchOfficiallyEnded() = {
    matchEndedSubject.onNext(true)
  }

  def raiseBeginNewMath() = {
    matchStarted = true
    beginNewMatchSubject.onNext(true)
  }

  def raisePlayerHurtEvent(playerHurtEvent: PlayerHurtEvent) = {
    playerHurtSubject.onNext(playerHurtEvent)
  }
}

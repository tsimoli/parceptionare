package parsers.main

import models.Player
import parsers.main.Enums.Weapon.Weapon

object Enums {
  object NET_Messages extends Enumeration {
    val net_NOP = 0
    val net_Disconnect = 1
    val net_File = 2
    val net_SplitScreenUser = 3
    val net_Tick = 4
    val net_StringCmd = 5
    val net_StringConVar = 6
    val net_SignonState = 7
  }

  object SVC_Messages extends Enumeration {
    val svc_ServerInfo = Value(8)
    val svc_SendTable = Value(9)
    val svc_ClassInfo = 10
    val svc_SetPause = 11
    val svc_CreateStringTable = 12
    val svc_UpdateStringTable = 13
    val svc_VoiceInit = 14
    val svc_VoiceData = 15
    val svc_Print = 16
    val svc_Sounds = 17
    val svc_SetView = 18
    val svc_FixAngle = 19
    val svc_CrosshairAngle = 20
    val svc_BSPDecal = 21
    val svc_SplitScreen = 22
    val svc_UserMessage = 23
    val svc_EntityMessage = 24
    val svc_GameEvent = 25
    val svc_PacketEntities = 26
    val svc_TempEntities = 27
    val svc_Prefetch = 28
    val svc_Menu = 29
    val svc_GameEventList = 30
    val svc_GetCvarValue = 31
    val svc_PainmapData = 33
    val svc_CmdKeyValues = 34
    val svc_EncryptedData = 35
  }

  object SendPropertyType {
    val int: Int = 0
    val float = 1
    val vector = 2
    val vectorXY = 3
    val string = 4
    val array = 5
    val dataTable = 6
    val int64 = 7
  }

  object Weapon extends Enumeration {
    
    type Weapon = Value
    
    val Unknown = Value(0)

    //Pistoles
    val P2000 = Value(1)
    val Glock = Value(2)
    val P250 = Value(3)
    val Deagle = Value(4)
    val FiveSeven = Value(5)
    val DualBarettas = Value(6)
    val Tec9 = Value(7)
    val CZ = Value(8)
    val USP = Value(9)
    val Revolver = Value(10)

    //SMGs
    val MP7 = Value(101)
    val MP9 = Value(102)
    val Bizon = Value(103)
    val Mac10 = Value(104)
    val UMP = Value(105)
    val P90 = Value(106)

    //Heavy
    val SawedOff = Value(201)
    val Nova = Value(202)
    val Mag7 = Value(203)
    val XM1014 = Value(204)
    val M249 = Value(205)
    val Negev = Value(206)

    //Rifle
    val Gallil = Value(301)
    val Famas = Value(302)
    val AK47 = Value(303)
    val M4A4 = Value(304)
    val M4A1 = Value(305)
    val Scout = Value(306)
    val SG556 = Value(307)
    val AUG = Value(308)
    val AWP = Value(309)
    val Scar20 = Value(310)
    val G3SG1 = Value(311)

    //Equipment
    val Zeus = Value(401)
    val Kevlar = Value(402)
    val Helmet = Value(403)
    val Bomb = Value(404)
    val Knife = Value(405)
    val DefuseKit = Value(406)
    val World = Value(407)

    //Grenades
    val Decoy = Value(501)
    val Molotov = Value(502)
    val Incendiary = Value(503)
    val Flash = Value(504)
    val Smoke = Value(505)
    val HE = Value(506)
  }
  
  object EquipmentElement {

    def mapEquipment(equipmentName: String) = {
      var weapon = Weapon.Unknown

      if (equipmentName.contains("knife") || equipmentName.replace("weapon_", "") == "bayonet") {
        weapon = Weapon.Knife
      }

      if (weapon == Weapon.Unknown) {
        equipmentName.replace("weapon_", "") match {
          case "ak47" | "manifest" => weapon = Weapon.AK47
          case "aug" => weapon = Weapon.AUG
          case "awp" => weapon = Weapon.AWP
          case "bizon" => weapon = Weapon.Bizon
          case "c4" => weapon = Weapon.Bomb
          case "deagle" => weapon = Weapon.Deagle
          case "decoy" | "decoygrenade" => weapon = Weapon.Decoy
          case "elite" => weapon = Weapon.DualBarettas
          case "famas" => weapon = Weapon.Famas
          case "fiveseven" => weapon = Weapon.FiveSeven
          case "flashbang" => weapon = Weapon.Flash
          case "g3sg1" => weapon = Weapon.G3SG1
          case "galil" | "galilar" => weapon = Weapon.Gallil
          case "glock" => weapon = Weapon.Glock
          case "hegrenade" => weapon = Weapon.HE
          case "hkp2000" => weapon = Weapon.P2000
          case "incgrenade" | "incendiarygrenade" => weapon = Weapon.Incendiary
          case "m249" => weapon = Weapon.M249
          case "m4a1" => weapon =Weapon. M4A4
          case "mac10" => weapon = Weapon.Mac10
          case "mag7" => weapon = Weapon.Mag7
          case "molotov" | "molotovgrenade" => weapon = Weapon.Molotov
          case "mp7" => weapon = Weapon.MP7
          case "mp9" => weapon = Weapon.MP9
          case "negev" => weapon = Weapon.Negev
          case "nova" => weapon = Weapon.Nova
          case "p250" => weapon = Weapon.P250
          case "p90" => weapon = Weapon.P90
          case "sawedoff" => weapon = Weapon.SawedOff
          case "scar20" => weapon = Weapon.Scar20
          case "sg556" => weapon = Weapon.SG556;
          case "smokegrenade" => weapon = Weapon.Smoke
          case "ssg08" => weapon = Weapon.Scout
          case "taser" => weapon = Weapon.Zeus
          case "tec9" => weapon = Weapon.Tec9
          case "ump45" => weapon =Weapon. UMP;
          case "xm1014" => weapon = Weapon.XM1014
          case "m4a1_silencer" | "m4a1_silencer_off" => weapon = Weapon.M4A1
          case "cz75a" => weapon = Weapon.CZ
          case "usp" | "usp_silencer_off" | "usp_silencer" => weapon = Weapon.USP
          case "revolver" => weapon = Weapon.Revolver
          case "world" => weapon = Weapon.World
          case "inferno" => weapon = Weapon.Incendiary
          case "scar17" | "sg550" | "mp5navy" | "p228" | "scout" | "sg552" | "tmp" => weapon = Weapon.Unknown
          case _ => println("Unknown weapon: " + equipmentName); equipmentName.replace("weapon_", "")
        }
      }
      weapon
    }

    def mapEquipementToEquipmentType(equipment: Equipment) = {
      equipment.weapon match {
        case Weapon.P2000 | Weapon.Glock | Weapon.P250 | Weapon.Deagle | Weapon.FiveSeven | Weapon.DualBarettas |  Weapon.Tec9 |  Weapon.CZ |  Weapon.USP | Weapon.Revolver => EquipmentClass.Pistol
        case Weapon.MP7 | Weapon.MP9 | Weapon.Bizon | Weapon.Mac10 |Weapon.UMP | Weapon.P90 => EquipmentClass.SMG
        case Weapon.SawedOff | Weapon.Nova | Weapon.Mag7 | Weapon.XM1014 |Weapon.M249 | Weapon.Negev => EquipmentClass.Heavy
        case Weapon.Gallil | Weapon.Famas | Weapon.AK47 | Weapon.M4A4 |Weapon.M4A1 | Weapon.SG556 | Weapon.AUG => EquipmentClass.Rifle
        case Weapon.Scout | Weapon.AWP | Weapon.Scar20 | Weapon.G3SG1 => EquipmentClass.Sniper
        case Weapon.Decoy | Weapon.Molotov | Weapon.Incendiary | Weapon.Flash | Weapon.Smoke | Weapon.HE => EquipmentClass.Grenade
        case Weapon.Zeus | Weapon.Kevlar | Weapon.Helmet | Weapon.Bomb | Weapon.Knife | Weapon.DefuseKit | Weapon.World => EquipmentClass.Equipment
        case _ => EquipmentClass.Unknown
      }
    }
  }

  object EquipmentClass {
    val Unknown = 0
    val Pistol = 1
    val SMG = 2
    val Heavy = 3
    val Rifle = 4
    val Equipment = 5
    val Grenade = 6
    val Sniper = 7
  }

  case class Equipment() {
    var originalString = ""
    var skinID = ""
    var entityID: Int = 0

    var weapon: Weapon = Weapon.Unknown
    
    def equipmentClass(): Int = {
//      (weapon.asInstanceOf[Int] / 100) + 1
      EquipmentElement.mapEquipementToEquipmentType(this)
    }

    var ammoInMagazine: Int = -1

    var ammoType: Int = 0

    var owner: Option[Player] = None

    def reserveAmmo() {
      owner.get.ammoLeft(ammoType)
    }

    var equipment: Int = 0
  }
}


package parsing

import java.io.{File, FileInputStream}
import javax.inject.Inject

import actors.{CheckQueue, DecrementParsingCount, ShareCodeQueueActor}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Guice, PrivateModule}
import com.google.protobuf.CodedInputStream
import com.typesafe.config.ConfigFactory
import databasemodels._
import models.Events._
import models.{RoundStat, _}
import org.joda.time.DateTime
import parsers.DemoParser
import play.api.libs.concurrent.Akka
import play.api.{Logger, Play}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import services._
import util._
import databasemodels.PlayerDemoStats._
import net.codingwell.scalaguice.{ScalaModule, ScalaPrivateModule}
import slick.dbio.Effect.Transactional

import scala.collection.immutable.HashMap
import scala.concurrent.duration.DurationInt

object Parser {
  var matchStarted = false
  var roundStats = List[RoundStat]()
  var roundNumber = 1
  var currentRoundStat = RoundStat(roundNumber, List(), "", 0, "", highlight = false, 0, 0, 0, 0)
  var possibleCTCluther: Option[Player] = None
  var possibleTCluther: Option[Player] = None
  var TclutchType: Option[ClutchType.Value] = None
  var CTclutchType: Option[ClutchType.Value] = None
  var entryKiller: Option[Player] = None
  var hitMap = List[PlayerHitMap]()
  var possibleCTSurrender = 0
  var possibleTSurrender = 0
  var header: Option[Header] = None
  var demoParser: DemoParser = _
  var rankUpdates: RankUpdateEvent = RankUpdateEvent(List())
  var parseData: ParseData = _
  var utilityAtBeginningOfRound: HashMap[String, Int] = HashMap[String, Int]()
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  // Database injections

  val injector = Guice.createInjector(new MyModule(), new MyPrivateModule)

  import net.codingwell.scalaguice.InjectorExtensions._

  lazy val demoDataDao = injector.instance[DemoDataDao]
  lazy val playerDemoRefDao = injector.instance[PlayerDemoRefDao]
  lazy val playerProfileDao = injector.instance[PlayerProfileDao]
  lazy val playerRatingDao = injector.instance[PlayerRatingDao]
  lazy val playerRankDao = injector.instance[PlayerRankDao]
  lazy val shareCodeDao = injector.instance[ShareCodeDao]
  lazy val parseQueueDao = injector.instance[ParseQueueDao]
  lazy val playerDemoStatsDao = injector.instance[PlayerDemoStatsDao]
  val conf = ConfigFactory.load()

  def startParsing(parseData: ParseData, file: TemporaryFile) = {
      val input = new FileInputStream(file.file)
      val is = CodedInputStream.newInstance(input)
      is.setSizeLimit(Integer.MAX_VALUE)
      demoParser = new DemoParser(is)
      this.parseData = parseData
      shareCodeDao.updateShareCodeStatus(parseData.shareCode, "parsing_started")
      // Attach to events before parsing
      attachEvents()
      println("Parsing started: " + new DateTime().toString)
      if (demoParser.parseDemo()) {
        CloudStorage.uploadFile(file.file, parseData.matchId + ".dem")
        input.close()
        new File(file.file.getAbsolutePath).delete()
        shareCodeDao.updateShareCodeStatus(parseData.shareCode, "parsed")
        parseQueueDao.updateQueueItemStatus(parseData.shareCode, "parsed")
        println("Demo parsed: " + new DateTime().toString)
        true
      }
      else {
        shareCodeDao.updateShareCodeStatus(parseData.shareCode, "parsing_failed")
        println("Demo parsing failed or demo is already parsed")
        false
      }
  }

  def attachEvents() = {
    headerParsed
    beginMatch
    playerDeath
    roundMvp
    roundEnd
    roundOfficiallyEnd
    freezeTimeEnd
    roundStart
    bombPlanted
    bombDefused
    playerHurt
    rankUpdate
    matchEnd
    flashDetonate
    decoyStarted
    smokeDetonate
    infernoStarted
    HEDetonate
  }

  def createRoundStatsForPlayers(demoParser: DemoParser): List[PlayerRoundStats] = {
    demoParser.players.flatten.toList.map(player => PlayerRoundStats(
      roundNumber,
      player.steamID,
      player.steamId64bit,
      player.name,
      player.team,
      entryKill = false,
      List(),
      0,
      died = false,
      None,
      None,
      0,
      0,
      bombPlanted = false,
      bombDefused = false,
      0,
      0,
      None,
      None
    ))
  }

  def setCurrentRoundStat(demoParser: DemoParser): Unit = {
    currentRoundStat = RoundStat(roundNumber, createRoundStatsForPlayers(demoParser), "", 0, "", highlight = false, 0, 0, 0, 0)
  }

  def setHitMap(demoParser: DemoParser) = {
    hitMap = demoParser.players.flatten.map(player =>
      PlayerHitMap(player.steamID, player.name)
    ).toList
  }

  def mapReasonToString(reason: Int): String = {
    reason match {
      case 1 => "Target Bombed"
      case 7 => "Bomb Defused"
      case 8 => "CT Win"
      case 9 => "Terrorists Win"
      case 17 => possibleTSurrender = 1; "Terrorists Surrender"
      case 18 => possibleCTSurrender = 1; "CT Surrender"
      case i: Int => "Unknown Reason: " + i
    }
  }

  def headerParsed = {
    demoParser.headerParsed.subscribe(headerParded => {
      header = Some(headerParded.header)
    })
  }

  def beginMatch = {
    demoParser.beginNewMatchSubject.subscribe(startMatch => {
      matchStarted = true
      setCurrentRoundStat(demoParser)
      currentRoundStat = currentRoundStat.copy(CTStartMoney = 4000, TStartMoney = 4000)
      demoParser.players.flatten.toList.foreach { player =>
        findPlayerRoundStat(player.steamID).foreach { playerStat =>
          updateCurrentRoundPlayerStatList(playerStat.copy(startMoney = 800))
        }
      }
      roundNumber = 1
      roundStats = List()
      entryKiller = None
      TclutchType = None
      CTclutchType = None
      possibleCTCluther = None
      possibleTCluther = None

      setHitMap(demoParser)
    })
  }

  def playerDeath = {
    demoParser.playerDeathSubject.subscribe(playerDeath => {
      if (matchStarted) {
        var entryDuel = false
        playerDeath.killer.foreach { killer =>
          findPlayer(killer.steamID).foreach { foundKiller =>
            findPlayerRoundStat(killer.steamID).foreach { playerRoundStat =>
              playerDeath.dead.foreach { dead =>
                findPlayer(dead.steamID).foreach { foundDead =>
                  val weaponType = playerDeath.weaponUsed.equipmentClass() match {
                    case 1 => "Pistol"
                    case 2 => "SMG"
                    case 3 => "Heavy"
                    case 4 => "Rifle"
                    case 7 => "Sniper"
                    case 5 => "Equipment"
                    case 6 => "Grenade"
                  }
                  val kills = Kill(foundDead.name, PlayerPosition(foundKiller.positionX, foundKiller.positionY, foundKiller.positionZ), PlayerPosition(foundDead.positionX, foundDead.positionY, foundDead.positionZ), playerDeath.weaponUsed.weapon.toString, weaponType, playerDeath.headShot, foundKiller.lastPlace, playerDeath.penetratedObjects) :: playerRoundStat.kills
                  if (foundKiller.teamId != foundDead.teamId) {
                    if (entryKiller.isEmpty) {
                      updateCurrentRoundPlayerStatList(playerRoundStat.copy(entryKill = true, kills = kills))
                      entryKiller = playerDeath.killer
                      entryDuel = true
                    } else updateCurrentRoundPlayerStatList(playerRoundStat.copy(kills = kills))

                    findPlayerRoundStat(foundDead.steamID).foreach { deadPlayerRoundStat =>
                      if (entryKiller.nonEmpty && entryDuel) {
                        entryDuel = false
                        updateCurrentRoundPlayerStatList(deadPlayerRoundStat.copy(died = true, entryDuel = Some(true)))
                      } else updateCurrentRoundPlayerStatList(deadPlayerRoundStat.copy(died = true))
                    }
                  }
                  checkClutch()
                }
              }
            }
          }
        }
        playerDeath.assister.foreach { assister =>
          findPlayer(assister.steamID).foreach { foundAssister =>
            findPlayerRoundStat(foundAssister.steamID).foreach { playerRoundStat =>
              updateCurrentRoundPlayerStatList(playerRoundStat.copy(assists = playerRoundStat.assists + 1))
            }
          }
        }
      }
    })
  }

  def roundMvp = {
    demoParser.roundMVPSubject.subscribe(roundMvp => {
      if (matchStarted) {
        val reason = roundMvp.reason match {
          case Some(1) => MvpType.mostKills
          case Some(2) => MvpType.bombDefused
          case Some(3) => MvpType.bombPlanted
          case _ => MvpType.bombPlanted
        }
        roundMvp.player.foreach { mvpPlayer =>
          findPlayerRoundStat(mvpPlayer.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(mvpType = Some(reason)))
          }
        }
      }
    })
  }

  def roundEnd = {
    demoParser.roundEndSubject.subscribe(roundEndEvent => {
      if (matchStarted) {
        val isHighLight = checkHighLight(roundEndEvent)
        val winnerTeam = roundEndEvent.winnerTeam map {
          case 3 => "CT"
          case 2 => "T"
          case _ => ""
        }
        val winningTeam = if (roundNumber < 16 && winnerTeam.get == "CT") 1 else if (roundNumber < 16 && winnerTeam.get == "T") 2 else if (roundNumber >= 16 && winnerTeam.get == "CT") 2 else 1
        currentRoundStat = currentRoundStat.copy(highlight = isHighLight, winningSide = winnerTeam.getOrElse(""), winningTeam = winningTeam, reason = mapReasonToString(roundEndEvent.reason.getOrElse(-1)))
        clearRound()
      }
    })
  }

  private def clearRound(): Unit = {
    possibleCTCluther = None
    possibleTCluther = None
    TclutchType = None
    CTclutchType = None
    entryKiller = None
    utilityAtBeginningOfRound = HashMap[String, Int]()
  }

  def roundOfficiallyEnd = {
    demoParser.roundOfficiallyEnd.subscribe(onNext = officiallyEndEvent => {
      if (matchStarted) {
        roundStats = currentRoundStat :: roundStats
        roundNumber = roundNumber + 1
        if (roundNumber == 16) {
          switchTeams()
        }
      }
    })
  }

  def freezeTimeEnd = {
    demoParser.freezeTimeEndSubject.subscribe(freezeTimeEndEvent => {
      demoParser.players.flatten.toList.foreach { player =>
        val utilityCount = player.rawWeapons.toList.map { keyEquipment =>
          keyEquipment._2.equipmentClass() match {
            case 6 => 1
            case _ => 0
          }
        }
        findPlayerRoundStat(player.steamID).foreach { playerStat =>
          val utilityAtBeginning = utilityAtBeginningOfRound.getOrElse(player.steamID, 0)
          val utilityBought = utilityCount.sum - utilityAtBeginning
          updateCurrentRoundPlayerStatList(playerStat.copy(equipmentValue = player.currentEquipmentValue, utilityBought = utilityBought))
        }
      }
      val CTRoundStartEquipmentValue = demoParser.players.flatten.toList.filter(_.teamId == 3).map(_.currentEquipmentValue).sum
      val TRoundStartEquipmentValue = demoParser.players.flatten.toList.filter(_.teamId == 2).map(_.currentEquipmentValue).sum
      currentRoundStat = currentRoundStat.copy(CTEquipmentValue = CTRoundStartEquipmentValue, TEquipmentValue = TRoundStartEquipmentValue)
    })
  }

  def roundStart = {
    demoParser.roundStartSubject.subscribe(roundStartEvent => {
      setCurrentRoundStat(demoParser)
      clearRound()
      if (roundNumber == 1 || roundNumber == 16) {
        demoParser.players.flatten.toList.foreach { player =>
          findPlayerRoundStat(player.steamID).foreach { playerStat =>
            updateCurrentRoundPlayerStatList(playerStat.copy(startMoney = 800))
          }
        }
        currentRoundStat = currentRoundStat.copy(CTStartMoney = 4000, TStartMoney = 4000)
      } else {
        demoParser.players.flatten.toList.foreach { player =>
          findPlayerRoundStat(player.steamID).foreach { playerStat =>
            updateCurrentRoundPlayerStatList(playerStat.copy(startMoney = player.money))
          }
        }
        demoParser.players.flatten.toList.foreach { player =>
          val utilityCount = player.rawWeapons.toList.map { keyEquipment =>
            keyEquipment._2.equipmentClass() match {
              case 6 => 1
              case _ => 0
            }
          }
          utilityAtBeginningOfRound += player.steamID -> utilityCount.sum
        }
        val CTStartMoneyValue = demoParser.players.flatten.toList.filter(_.teamId == 3).map(_.money).sum
        val TRoundStartValue = demoParser.players.flatten.toList.filter(_.teamId == 2).map(_.money).sum
        currentRoundStat = currentRoundStat.copy(CTStartMoney = CTStartMoneyValue, TStartMoney = TRoundStartValue)
      }
    })
  }

  def switchTeams() = {
    demoParser.players.flatten.toList.foreach {
      player =>
        if (player.teamId == 2) demoParser.players.updated(player.userId, Some(player.teamId = 3, player.team = "CT"))
        else if (player.teamId == 3) demoParser.players.updated(player.userId, Some(player.teamId = 2, player.team = "TERRORIST"))
    }
  }

  /*
    Returns tuple (win, lost, tied) value
   */
  def determineIfPlayerWonGame(steamId: String, teams: Teams) = {
    val matchWinnerTeam = getWinnerTeam
    val playerTeam = getPlayerTeam(steamId, teams)
    if (playerTeam == matchWinnerTeam) (1, 0, 0) else if (matchWinnerTeam == 0) (0, 0, 1) else (0, 1, 0)
  }

  def getWinnerTeam = {
    if (demoParser.TScore > demoParser.CTScore || possibleCTSurrender == 1) 1 else if (demoParser.TScore < demoParser.CTScore || possibleTSurrender == 1) 2 else 0
  }

  def getPlayerTeam(steamId: String, teams: Teams) = {
    if (teams.team1.exists(a => a.steamId == steamId)) 1 else 2
  }

  def rankUpdate = {
    demoParser.rankUpdateSubject.subscribe(rankUpdateEvent => {
      if (rankUpdates.rankUpdates.isEmpty)
        rankUpdates = rankUpdateEvent
    })
  }

  def matchEnd = demoParser.matchEndedSubject.subscribe(matchEndEvent => {
    println("Match End")
    roundStats = currentRoundStat :: roundStats
    val ratings: List[HLTVRating] = HLTVRatingCalc.calculateRatingsAndStore(roundStats, demoParser)
    val team1 = demoParser.players.flatten.toList.filter(_.teamId == 2).flatMap(p => p.additionalInformation.map(aInfo =>
      AdditionalPlayerStats(p.steamID, p.steamId64bit, p.name, aInfo.kills, aInfo.assists, aInfo.deaths, aInfo.score, ratings.find(_.steamId == p.steamID).map(_.rating).getOrElse(0))))
    val team2 = demoParser.players.flatten.toList.filter(_.teamId == 3).flatMap(p => p.additionalInformation.map(aInfo =>
      AdditionalPlayerStats(p.steamID, p.steamId64bit, p.name, aInfo.kills, aInfo.assists, aInfo.deaths, aInfo.score, ratings.find(_.steamId == p.steamID).map(_.rating).getOrElse(0))))

    val teams = Teams(team1, team2)

    implicit val htlvRatingFormat = Json.format[HLTVRating]
    val matchData = Json.obj("matchDuration" -> parseData.matchDuration, "teams" -> Json.toJson(teams), "hltvRatings" -> Json.toJson(ratings), "hitMaps" -> Json.toJson(hitMap), "rounds" -> Json.toJson(roundStats))
    val demoUrl = conf.getString("google.storage.basic.url") + parseData.matchId + ".dem"
    val demoData = DemoData(0, parseData.matchId, parseData.shareCode, demoUrl, new DateTime(parseData.matchDate * 1000L), matchData, header.get.mapName, demoParser.TScore, demoParser.CTScore, new DateTime())
    insertDemoData(teams, demoData, ratings)
    println("Finished parsing demo with id: " + parseData.matchId)
  })

  def insertDemoData(teams: Teams, demoData: DemoData, ratings: List[HLTVRating]): Unit = {
    demoDataDao.insertDemo(demoData).map { insertedDemoData =>
      demoParser.players.flatten.toList.foreach { player =>
        val playerRoundStats = roundStats.flatMap(roundStat => roundStat.playerStats.filter(p => p.steamId == player.steamID))
        val aggregatedPlayerRoundStats = PlayerDemoStatsAggregator.aggregatePlayersStats(player, playerRoundStats)
        val adr = hitMap.find(_.steamId == player.steamID).map { playerHitMap =>
          playerHitMap.totalDamageDone / roundNumber.toFloat
        }.getOrElse(0.toFloat)
        val kpr = playerRoundStats.map(roundStat => roundStat.kills.size).sum / roundNumber.toFloat
        val kdRatio = playerRoundStats.map(roundStat => roundStat.kills.size).sum.toFloat / playerRoundStats.map(roundStat => if (roundStat.died) 1 else 0).sum.toFloat
        implicit val utilityUsageRatioFormat = Json.format[UtilityUsage]
        implicit val entrykillDuelWinRatiosFormat = Json.format[EntryDuels]
        implicit val clutchRatiosFormat = Json.format[Clutches]
        implicit val killCountFormat = Json.format[KillCount]
        val rankUpdate = rankUpdates.rankUpdates.reverse.find {
          _.accountId == SteamIdUtil.transformSteamIdTo64bit(player.steamID)
        }.get

        val playerDemoStats = PlayerDemoStats(
          0,
          insertedDemoData.id,
          player.name,
          player.steamID,
          player.steamId64bit,
          rankUpdate.wins,
          Json.toJson(KillCount(aggregatedPlayerRoundStats.playerKillsByWeaponType.rifleKills,
            aggregatedPlayerRoundStats.playerKillsByWeaponType.pistolKills,
            aggregatedPlayerRoundStats.playerKillsByWeaponType.smgKills,
            aggregatedPlayerRoundStats.playerKillsByWeaponType.heavyKills,
            aggregatedPlayerRoundStats.playerKillsByWeaponType.sniperKills)),
          kdRatio.toFloat,
          adr,
          kpr,
          rankUpdate.rankNew,
          Json.toJson(aggregatedPlayerRoundStats.utilityUsageRatio),
          Json.toJson(aggregatedPlayerRoundStats.entrykillDuelWinRatios),
          Json.toJson(aggregatedPlayerRoundStats.clutchRatios),
          new DateTime()
        )
        playerDemoStatsDao.insert(playerDemoStats)
      }
      demoParser.players.flatten.toList.foreach(player => playerDemoRefDao.insertRef(PlayerDemoRef(-1, player.steamId64bit, insertedDemoData.id)))
      ratings.foreach { hltvRating =>
        playerRatingDao.insertRating(PlayerRating(-1, hltvRating.steamId, SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId), hltvRating.rating.toFloat, insertedDemoData.id, new DateTime())).map { _ =>
          playerRatingDao.fetchAllBySteamId(hltvRating.steamId).map { ratings =>
            val playerRatingAverage = ratings.map(rating => rating.rating).sum / ratings.size
            playerProfileDao.findWithSteamId(hltvRating.steamId).map { playerProfileOption =>
              playerProfileOption.map { playerProfile =>
                updatePlayerProfile(playerProfile, teams, hltvRating, playerRatingAverage, insertedDemoData)
              }.getOrElse {
                insertNewPlayerProfile(teams, hltvRating, insertedDemoData)
              }
            }
          }
        }
      }
    }
  }

  def insertNewPlayerProfile(teams: Teams, hltvRating: HLTVRating, insertedDemoData: DemoData): Option[Any] = {
    findPlayer(hltvRating.steamId).map { foundPlayer =>
      val gameResultTupleForPlayer = determineIfPlayerWonGame(foundPlayer.steamID, teams)
      val winnerTeamScore = if (getWinnerTeam == 1) demoParser.TScore else if (getWinnerTeam == 2) demoParser.CTScore else 15
      val roundsWon = if (getWinnerTeam == getPlayerTeam(foundPlayer.steamID, teams)) winnerTeamScore else 30 - winnerTeamScore
      val rankUpdate = rankUpdates.rankUpdates.reverse.find {
        _.accountId == SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId)
      }.get
      playerRankDao.insertRank(PlayerRank(-1, hltvRating.steamId, SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId), rankUpdate.wins, rankUpdate.rankNew, insertedDemoData.id, new DateTime(parseData.matchDate * 1000L), new DateTime()))
      playerProfileDao.insertPlayerProfile(PlayerProfile(-1, hltvRating.steamId,
        SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId),
        Some(foundPlayer.name),
        1,
        roundStats.size,
        gameResultTupleForPlayer._1,
        gameResultTupleForPlayer._3,
        roundsWon,
        foundPlayer.additionalInformation.get.kills,
        foundPlayer.additionalInformation.get.deaths,
        foundPlayer.additionalInformation.get.assists,
        roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.entryKill) 1 else 0)).sum,
        hitMap.find(_.steamId == hltvRating.steamId).get.totalDamageDone,
        foundPlayer.additionalInformation.get.mvps,
        hltvRating.rating.toFloat,
        roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.bombPlanted) 1 else 0)).sum,
        roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.bombDefused) 1 else 0)).sum,
        roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.clutch.nonEmpty) 1 else 0)).sum,
        new DateTime(),
        new DateTime()
      ))
    }
  }

  def updatePlayerProfile(playerProfile: PlayerProfile, teams: Teams, hltvRating: HLTVRating, playerRatingAverage: Float, insertedDemoData: DemoData) = {
    findPlayer(hltvRating.steamId).map { foundPlayer =>
      val gameResultTupleForPlayer = determineIfPlayerWonGame(foundPlayer.steamID, teams)
      val winnerTeamScore = if (getWinnerTeam == 1) demoParser.TScore else if (getWinnerTeam == 2) demoParser.CTScore else 15
      val roundsWon = if (getWinnerTeam == getPlayerTeam(foundPlayer.steamID, teams)) winnerTeamScore else 30 - winnerTeamScore
      val rankUpdate = rankUpdates.rankUpdates.reverse.find {
        _.accountId == SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId)
      }.get
      playerRankDao.insertRank(PlayerRank(-1, hltvRating.steamId, SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId), rankUpdate.wins, rankUpdate.rankNew, insertedDemoData.id, new DateTime(parseData.matchDate * 1000L), new DateTime()))
      playerProfileDao.updatePlayerProfile(PlayerProfile(-1, hltvRating.steamId,
        SteamIdUtil.transformSteamIdTo64bit(hltvRating.steamId),
        Some(foundPlayer.name),
        playerProfile.playedGames + 1,
        playerProfile.playedRounds + roundStats.size,
        playerProfile.gamesWon + gameResultTupleForPlayer._1,
        playerProfile.gamesTied + gameResultTupleForPlayer._3,
        playerProfile.roundsWon + roundsWon,
        playerProfile.kills + foundPlayer.additionalInformation.get.kills,
        playerProfile.deaths + foundPlayer.additionalInformation.get.deaths,
        playerProfile.assists + foundPlayer.additionalInformation.get.assists,
        playerProfile.entryKills + roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.entryKill) 1 else 0)).sum,
        playerProfile.damageDone + hitMap.find(_.steamId == hltvRating.steamId).get.totalDamageDone,
        playerProfile.mvps + foundPlayer.additionalInformation.get.mvps,
        playerRatingAverage,
        playerProfile.plants + roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.bombPlanted) 1 else 0)).sum,
        playerProfile.defuses + roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.bombDefused) 1 else 0)).sum,
        playerProfile.clutchesWon + roundStats.flatMap(_.playerStats.filter(_.steamId == hltvRating.steamId).map(stat => if (stat.clutch.nonEmpty) 1 else 0)).sum,
        playerProfile.created,
        new DateTime()
      ))
    }
  }

  def bombPlanted = {
    demoParser.bombPlantedSubject.subscribe(bombPlantedEvent => {
      bombPlantedEvent.player.foreach { planter =>
        findPlayerRoundStat(planter.steamID).foreach { playerStat =>
          updateCurrentRoundPlayerStatList(playerStat.copy(bombPlanted = true))
        }
      }
    }
    )
  }

  def bombDefused = {
    demoParser.bombDefusedSubject.subscribe(bombdefusedEvent => {
      bombdefusedEvent.player.foreach { defuser =>
        findPlayerRoundStat(defuser.steamID).foreach { playerStat =>
          updateCurrentRoundPlayerStatList(playerStat.copy(bombDefused = true))
        }
      }
    })
  }

  def playerHurt = {
    demoParser.playerHurtSubject.subscribe(playerHurtEvent => {
      if (matchStarted) {
        playerHurtEvent.attacker.foreach { attacker =>
          hitMap.find(_.steamId == attacker.steamID).map { playerHitMap =>
            val updateIndex = hitMap.indexWhere(_.steamId == attacker.steamID)
            playerHurtEvent.hitGroup match {
              case HitGroup.Generic => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(generic = playerHitMap.generic + 1, sum = playerHitMap.sum + 1))
              case HitGroup.Head => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(head = playerHitMap.head + 1, sum = playerHitMap.sum + 1))
              case HitGroup.Chest => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(chest = playerHitMap.chest + 1, sum = playerHitMap.sum + 1))
              case HitGroup.Stomach => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(stomach = playerHitMap.stomach + 1, sum = playerHitMap.sum + 1))
              case HitGroup.LeftArm => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(leftArm = playerHitMap.leftArm + 1, sum = playerHitMap.sum + 1))
              case HitGroup.RightArm => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(rightArm = playerHitMap.rightArm + 1, sum = playerHitMap.sum + 1))
              case HitGroup.LeftLeg => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(leftLeg = playerHitMap.leftLeg + 1, sum = playerHitMap.sum + 1))
              case HitGroup.RightLeg => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(rightLeg = playerHitMap.rightLeg + 1, sum = playerHitMap.sum + 1))
              case HitGroup.Gear => hitMap = hitMap.updated(updateIndex, playerHitMap.copy(gear = playerHitMap.gear + 1, sum = playerHitMap.sum + 1))
            }
            hitMap = hitMap.updated(updateIndex, hitMap(updateIndex).copy(totalDamageDone = (playerHitMap.totalDamageDone + playerHurtEvent.healthDamage)))
          }.getOrElse {
            hitMap = PlayerHitMap(attacker.steamID, attacker.name) :: hitMap
          }
        }
      }
    })
  }

  def flashDetonate = {
    demoParser.flashDetonateSubject.subscribe(nadeEvent => {
      if (matchStarted) {
        nadeEvent.thrownBy.foreach { thrower =>
          findPlayerRoundStat(thrower.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(utilityUsed = foundPlayerStat.utilityUsed + 1))
          }
        }
      }
    })
  }

  def decoyStarted = {
    demoParser.decoyStartedSubject.subscribe(nadeEvent => {
      if (matchStarted) {
        nadeEvent.thrownBy.foreach { thrower =>
          findPlayerRoundStat(thrower.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(utilityUsed = foundPlayerStat.utilityUsed + 1))
          }
        }
      }
    })
  }

  def smokeDetonate = {
    demoParser.smokeDetonateSubject.subscribe(nadeEvent => {
      if (matchStarted) {
        nadeEvent.thrownBy.foreach { thrower =>
          findPlayerRoundStat(thrower.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(utilityUsed = foundPlayerStat.utilityUsed + 1))
          }
        }
      }
    })
  }

  def infernoStarted = {
    demoParser.infernoStartedSubject.subscribe(nadeEvent => {
      if (matchStarted) {
        nadeEvent.thrownBy.foreach { thrower =>
          findPlayerRoundStat(thrower.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(utilityUsed = foundPlayerStat.utilityUsed + 1))
          }
        }
      }
    })
  }

  def HEDetonate = {
    demoParser.HEDetonateSubject.subscribe(nadeEvent => {
      if (matchStarted) {
        nadeEvent.thrownBy.foreach { thrower =>
          findPlayerRoundStat(thrower.steamID).foreach { foundPlayerStat =>
            updateCurrentRoundPlayerStatList(foundPlayerStat.copy(utilityUsed = foundPlayerStat.utilityUsed + 1))
          }
        }
      }
    })
  }

  def findPlayer(steamId: String) = {
    demoParser.players.flatten.toList.find(_.steamID == steamId)
  }

  def findPlayerRoundStat(steamId: String) = {
    currentRoundStat.playerStats.find(_.steamId == steamId)
  }

  def updateCurrentRoundPlayerStatList(playerRoundStats: PlayerRoundStats) = {
    val updatedIndex = currentRoundStat.playerStats.indexWhere(_.steamId == playerRoundStats.steamId)
    currentRoundStat = currentRoundStat.copy(playerStats = currentRoundStat.playerStats.updated(updatedIndex, playerRoundStats))
  }

  def checkClutch() = {
    val CTTeamCount = demoParser.players.flatten.toList.count(p => p.teamId == 3 && p.isAlive)
    val TTeamCount = demoParser.players.flatten.toList.count(p => p.teamId == 2 && p.isAlive)
    if (CTTeamCount == 1 && possibleCTCluther.isEmpty && TTeamCount > 0) {
      possibleCTCluther = demoParser.players.flatten.toList.find(p => p.teamId == 3 && p.hp > 0)
      CTclutchType = Some(calculateClutchType(TTeamCount))
      possibleCTCluther.foreach { player =>
        findPlayerRoundStat(player.steamID).foreach { playerStat =>
          updateCurrentRoundPlayerStatList(playerStat.copy(possibleClutch = CTclutchType))
        }
      }
    }

    if (TTeamCount == 1 && possibleTCluther.isEmpty && CTTeamCount > 0) {
      possibleTCluther = demoParser.players.flatten.toList.find(p => p.teamId == 2 && p.hp > 0)
      TclutchType = Some(calculateClutchType(CTTeamCount))
      possibleTCluther.foreach { player =>
        findPlayerRoundStat(player.steamID).foreach { playerStat =>
          updateCurrentRoundPlayerStatList(playerStat.copy(possibleClutch = TclutchType))
        }
      }
    }
  }

  def calculateClutchType(playerAmount: Int) = {
    playerAmount match {
      case 1 => ClutchType.oneVsOne
      case 2 => ClutchType.oneVsTwo
      case 3 => ClutchType.oneVsThree
      case 4 => ClutchType.oneVsFour
      case 5 => ClutchType.oneVsFive
      case 0 => ClutchType.oneVsOne
    }
  }

  def checkHighLight(roundEndEvent: RoundEndEvent): Boolean = {
    def checkClutches() = {
      if (roundEndEvent.winnerTeam.getOrElse(0) == 3 && possibleCTCluther.isDefined && CTclutchType.isDefined) {
        possibleCTCluther.foreach { clutcher =>
          findPlayerRoundStat(clutcher.steamID).foreach { roundStat =>
            updateCurrentRoundPlayerStatList(roundStat.copy(clutch = CTclutchType))
          }
        }
        true
      } else if (roundEndEvent.winnerTeam.getOrElse(0) == 2 && possibleTCluther.isDefined && TclutchType.isDefined) {
        possibleTCluther.foreach { clutcher =>
          findPlayerRoundStat(clutcher.steamID).foreach { roundStat =>
            updateCurrentRoundPlayerStatList(roundStat.copy(clutch = TclutchType))
          }
        }
        true
      } else {
        false
      }
    }
    def checkMultiKills(): Boolean = {
      currentRoundStat.playerStats.count(_.kills.size > 2) > 0
    }
    checkClutches() || checkMultiKills()
  }
}

class MyModule extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    bind[DemoDataDao].to[DemoDataDaoImpl]
    bind[PlayerProfileDao].to[PlayerProfileDaoImpl]
    bind[PlayerRatingDao].to[PlayerRatingDaoImpl]
    bind[PlayerRankDao].to[PlayerRankDaoImpl]
    bind[ShareCodeDao].to[ShareCodeDaoImpl]
    bind[ParseQueueDao].to[ParseQueueDaoImpl]
    bind[PlayerDemoStatsDao].to[PlayerDemoStatsDaoImpl]
  }
}

class MyPrivateModule extends PrivateModule with ScalaPrivateModule {
  def configure(): Unit = {

  }
}

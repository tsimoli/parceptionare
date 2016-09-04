package util

import databasemodels.{Clutches, EntryDuels, UtilityUsage}
import models.{ClutchType, Player, PlayerRoundStats, RoundStat}
import play.api.libs.json.JsValue

object PlayerDemoStatsAggregator {
  /*
    Returns tuple of UtilityUsageRatio, EntrykillDuelWinRatios and ClutchRatios
   */
  def aggregatePlayersStats(player: Player, playerRoundStats: List[PlayerRoundStats]) = {
    val utilityUsed = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.utilityUsed
    }.sum

    val utilityBought = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.utilityBought
    }.sum

    val entryDuelCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.entryDuel.map { entryDuel =>
        if(entryDuel) 1
        else 0
      }.getOrElse(0)
    }.sum

    val entryDuelCountCT = playerRoundStats.map { playerRoundStat =>
      if (playerRoundStat.entryKill && playerRoundStat.team == "CT") 1
      else 0
    }.sum

    val entryDuelCountT = playerRoundStats.map { playerRoundStat =>
      if (playerRoundStat.entryKill && playerRoundStat.team == "TERRORIST") 1
      else 0
    }.sum

    val oneVsOneCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.possibleClutch.map { possibleClutch =>
        if (possibleClutch == ClutchType.oneVsOne) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsOneWon = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.clutch.map { clutch =>
        if (clutch == ClutchType.oneVsOne) 1
        else 0
      }.getOrElse(0)
    }.sum
    val oneVsTwoCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.possibleClutch.map { possibleClutch =>
        if (possibleClutch == ClutchType.oneVsTwo) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsTwoWon = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.clutch.map { clutch =>
        if (clutch == ClutchType.oneVsTwo) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsThreeCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.possibleClutch.map { possibleClutch =>
        if (possibleClutch == ClutchType.oneVsThree) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsThreeWon = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.clutch.map { clutch =>
        if (clutch == ClutchType.oneVsThree) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsFourCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.possibleClutch.map { possibleClutch =>
        if (possibleClutch == ClutchType.oneVsFour) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsFourWon = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.clutch.map { clutch =>
        if (clutch == ClutchType.oneVsFour) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsFiveCount = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.possibleClutch.map { possibleClutch =>
        if (possibleClutch == ClutchType.oneVsFive) 1
        else 0
      }.getOrElse(0)
    }.sum

    val oneVsFiveWon = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.clutch.map { clutch =>
        if (clutch == ClutchType.oneVsFive) 1
        else 0
      }.getOrElse(0)
    }.sum

    val rifleKills = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.kills.map { kill =>
        if (kill.weaponType == "Rifle") 1
        else 0
      }.sum
    }.sum
    val pistolKills = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.kills.map { kill =>
        if (kill.weaponType == "Pistol") 1
        else 0
      }.sum
    }.sum

    val heavyKills = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.kills.map { kill =>
        if (kill.weaponType == "Heavy") 1
        else 0
      }.sum
    }.sum

    val smgKills = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.kills.map { kill =>
        if (kill.weaponType == "SMG") 1
        else 0
      }.sum
    }.sum

    val sniperKills = playerRoundStats.map { playerRoundStat =>
      playerRoundStat.kills.map { kill =>
        if (kill.weaponType == "Sniper") 1
        else 0
      }.sum
    }.sum

    PlayerDemoStatsAggregated(UtilityUsage(utilityBought, utilityUsed), EntryDuels(entryDuelCount + entryDuelCountCT + entryDuelCountT, entryDuelCountCT, entryDuelCountT), Clutches(oneVsOneCount, oneVsOneWon, oneVsTwoCount, oneVsTwoWon, oneVsThreeCount, oneVsThreeWon, oneVsFourCount, oneVsFourWon, oneVsFiveCount, oneVsFiveWon),
      PlayerKillsByWeaponType(pistolKills, smgKills, rifleKills, heavyKills, sniperKills))
  }
}

case class PlayerKillsByWeaponType(pistolKills: Int, smgKills: Int, rifleKills: Int, heavyKills: Int, sniperKills: Int)

case class PlayerDemoStatsAggregated(utilityUsageRatio: UtilityUsage, entrykillDuelWinRatios: EntryDuels, clutchRatios: Clutches, playerKillsByWeaponType: PlayerKillsByWeaponType)

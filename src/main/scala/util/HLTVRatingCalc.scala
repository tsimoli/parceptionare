package util

import models.RoundStat
import parsers.DemoParser
import play.api.libs.json.Json

case class HLTVRating(steamId: String, rating: Double)

object HLTVRating {
  implicit val HLTVRatingFormat = Json.format[HLTVRating]
}

object HLTVRatingCalc {
  /*
      (KillRating + 0.7*SurvivalRating + RoundsWithMultipleKillsRating)/2.7

      KillRating = Kills/Rounds/AverageKPR
      SurvivalRating = (Rounds-Deaths)/Rounds/AverageSPR
      RoundsWithMultipleKillsRating = (1K + 4*2K + 9*3K + 16*4K + 25*5K)/Rounds/AverageRMK

      AverageKPR = 0.679 (average kills per round)
      AverageSPR = 0.317 (average survived rounds per round)
      AverageRMK = 1.277 (average value calculated from rounds with multiple kills: (1K + 4*2K + 9*3K + 16*4K + 25*5K)/Rounds)

      1K = Number of rounds with 1 kill
      2K = Number of rounds with 2 kill
      3K = Number of rounds with 3 kill
      4K = Number of rounds with 4 kill
      5K = Number of rounds with 5 kill
    */

  def calculateRatingsAndStore(roundStats: List[RoundStat], demoParser: DemoParser) : List[HLTVRating] = {

    def findPlayerRoundStat(roundStat: RoundStat, steamId: String) = {
      roundStat.playerStats.find(_.steamId == steamId)
    }

    demoParser.players.flatten.toList.map { player =>
      var oneKillRounds = 0
      var twoKillRounds = 0
      var threeKillRounds = 0
      var fourKillRounds = 0
      var fiveKillRounds = 0
      var killsPerRound = 0.0
      var survivePerRound = 0.0

      roundStats.foreach { roundStat =>
        findPlayerRoundStat(roundStat, player.steamID).foreach { playerRoundStat =>
          playerRoundStat.kills.size match {
            case 1 => oneKillRounds = oneKillRounds + 1
            case 2 => twoKillRounds = twoKillRounds + 1
            case 3 => threeKillRounds = threeKillRounds + 1
            case 4 => fourKillRounds = fourKillRounds + 1
            case 5 => fiveKillRounds = fiveKillRounds + 1
            case _ =>
          }
        }
      }

      killsPerRound = roundStats.map { roundStat =>
        findPlayerRoundStat(roundStat, player.steamID).map { playerRoundStat =>
          playerRoundStat.kills.size
        }.getOrElse(0)
      }.sum.toDouble / roundStats.size

      survivePerRound = roundStats.map { roundStat =>
        findPlayerRoundStat(roundStat, player.steamID).map { playerRoundStat =>
          if (playerRoundStat.died) 0 else 1
        }.getOrElse(0)
      }.sum

      val survivalRating = (survivePerRound / roundStats.size) / 0.317
      val twoKillRoundsCalculated = 4 * twoKillRounds
      val threeKillRoundsCalculated = 9 * threeKillRounds
      val fourKillRoundsCalculated = 16 * fourKillRounds
      val fiveKillRoundsCalculated = 25 * fiveKillRounds

      val RoundsWithMultipleKillsRating = (oneKillRounds + twoKillRoundsCalculated + threeKillRoundsCalculated + fourKillRoundsCalculated + fiveKillRoundsCalculated).toDouble / roundStats.size / 1.277

      val rating = ((killsPerRound.toDouble / 0.679) + (0.7 * survivalRating.toDouble) + RoundsWithMultipleKillsRating).toDouble / 2.7
      HLTVRating(player.steamID, rating)
    }
  }
}

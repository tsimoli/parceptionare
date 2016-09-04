package databasemodels

import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class PlayerDemoStats(id: Long,
                           demoId: Long,
                           name: String,
                           steamId: String,
                           steam64bit: String,
                           mmMatchesWon: Int,
                           killCount: JsValue,
                           kdRatio: Float,
                           adr: Float,
                           kprRatio: Float,
                           rank: Int,
                           utilityUsageRatio: JsValue,
                           entryKillDuelWinRatios: JsValue,
                           clutchRatios: JsValue,
                           created: DateTime
                          )

case class UtilityUsage(utilityBought: Int, utilityUsed: Int)

case class EntryDuels(entryDuelCount: Int, CTDuelsWon: Int, TDuelsWon: Int)

case class Clutches(oneVsOneCount: Int,
                    oneVsOneWon: Int,
                    oneVsTwoCount: Int,
                    oneVsTwoWon: Int,
                    oneVsThreeCount: Int,
                    oneVsThreeWon: Int,
                    oneVsFourCount: Int,
                    oneVsFourWon: Int,
                    oneVsFiveCount: Int,
                    oneVsFiveWon: Int)

case class KillCount(rifleKills: Int,
                     pistolKills: Int,
                     smgKills: Int,
                     heavyKills: Int,
                     sniperKills: Int)
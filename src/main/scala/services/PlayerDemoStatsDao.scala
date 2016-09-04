package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import databasemodels.PlayerDemoStats
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._
import com.github.tototoshi.slick.PostgresJodaSupport._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future

trait PlayerDemoStatsComponent {

  class PlayerDemoStatss(tag: Tag) extends Table[PlayerDemoStats](tag, "player_demo_stats") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def demoId = column[Long]("demo_id")

    def name = column[String]("name")

    def steamId = column[String]("steam_id")

    def steam64bit = column[String]("steamid64bit")

    def mmMatchesWon = column[Int]("mm_matches_won")

    def killCount = column[JsValue]("kill_count")

    def kdRatio = column[Float]("kd_ratio")

    def adr = column[Float]("adr")

    def kprRatio = column[Float]("kpr_ratio")

    def rank = column[Int]("rank")

    def utilityUsageRatio = column[JsValue]("utility_usages")

    def entryKillDuels = column[JsValue]("entry_duels")

    def clutches = column[JsValue]("clutches")

    def created = column[DateTime]("created")

    def * = (
      id,
      demoId,
      name,
      steamId,
      steam64bit,
      mmMatchesWon,
      killCount,
      kdRatio,
      adr,
      kprRatio,
      rank,
      utilityUsageRatio,
      entryKillDuels,
      clutches,
      created) <>(PlayerDemoStats.tupled, PlayerDemoStats.unapply)
  }
}

@ImplementedBy(classOf[PlayerDemoStatsDaoImpl])
trait PlayerDemoStatsDao {
  def insert(playerDemoStats: PlayerDemoStats): Unit
}

class PlayerDemoStatsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends PlayerDemoStatsComponent with PlayerDemoStatsDao with HasDatabaseConfigProvider[JdbcProfile] {
  val playerDemoStatss = TableQuery[PlayerDemoStatss]

  def insert(playerDemoStats: PlayerDemoStats): Unit = {
      db.run(playerDemoStatss += playerDemoStats).map(_ => ())
  }
}
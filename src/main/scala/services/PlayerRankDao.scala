package services

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.ImplementedBy
import databasemodels._
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._

import scala.concurrent.Future

trait PlayerRankComponent {

  class PlayerRanks(tag: Tag) extends Table[PlayerRank](tag, "player_rank") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def steamId = column[String]("steam_id")
    def steamId64bit = column[Long]("steamid64bit")
    def mmMatchesWon = column[Int]("mm_matches_won")
    def rank = column[Int]("rank")
    def demoId = column[Long]("demo_id")
    def matchDate = column[DateTime]("match_date")
    def created = column[DateTime]("created")

    def * = (
      id,
      steamId,
      steamId64bit,
      mmMatchesWon,
      rank,
      demoId,
      matchDate,
      created) <>(PlayerRank.tupled, PlayerRank.unapply)
  }
}

@ImplementedBy(classOf[PlayerRankDaoImpl])
trait PlayerRankDao {
  def insertRank(playerRank: PlayerRank): Future[PlayerRank]
  def fetchAllBySteamId(steamId: String): Future[Seq[PlayerRank]]
}

class PlayerRankDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends PlayerRankComponent with PlayerRankDao with HasDatabaseConfigProvider[JdbcProfile] {

  val playerRanks= TableQuery[PlayerRanks]

  def insertRank(playerRank: PlayerRank) : Future[PlayerRank] = {
    val insertQuery = playerRanks returning playerRanks.map(_.id) into ((playerRank, id) => playerRank.copy(id = id))
    val action = insertQuery += playerRank
    db.run(action)
  }

  def fetchAllBySteamId(steamId: String) = {
    db.run(playerRanks.filter(_.steamId === steamId).result)
  }
}

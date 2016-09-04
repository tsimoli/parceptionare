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

trait PlayerRatingComponent {

  class PlayerRatings(tag: Tag) extends Table[PlayerRating](tag, "player_rating") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def steamId = column[String]("steam_id")
    def steamId64bit = column[Long]("steamid64bit")
    def rating = column[Float]("rating")
    def demoId = column[Long]("demo_id")
    def created = column[DateTime]("created")

    def * = (
      id,
      steamId,
      steamId64bit,
      rating,
      demoId,
      created) <>(PlayerRating.tupled, PlayerRating.unapply)
  }
}

@ImplementedBy(classOf[PlayerRatingDaoImpl])
trait PlayerRatingDao {
  def insertRating(playerRating: PlayerRating) : Future[PlayerRating]
  def fetchAllBySteamId(steamId: String): Future[Seq[PlayerRating]]
}

class PlayerRatingDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends PlayerRatingComponent with PlayerRatingDao with HasDatabaseConfigProvider[JdbcProfile] {

  val playerRatings= TableQuery[PlayerRatings]

  def insertRating(playerRating: PlayerRating) : Future[PlayerRating] = {
    val insertQuery = playerRatings returning playerRatings.map(_.id) into ((playerRating, id) => playerRating.copy(id = id))
    val action = insertQuery += playerRating
    db.run(action)
  }

  def fetchAllBySteamId(steamId: String) = {
    db.run(playerRatings.filter(_.steamId === steamId).result)
  }
}

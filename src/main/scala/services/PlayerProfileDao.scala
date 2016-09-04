package services

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.ImplementedBy
import databasemodels.{PlayerProfile, DemoData}
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._
import scala.concurrent.Future

import scala.concurrent.Future

trait PlayerProfileDaoComponent {

  class PlayerProfiles(tag: Tag) extends Table[PlayerProfile](tag, "player_profile") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def steamId = column[String]("steam_id")

    def steam64bit = column[Long]("steamid64bit")

    def name = column[Option[String]]("name")

    def playedGames = column[Long]("played_games")

    def playedRounds = column[Long]("played_rounds")

    def rank = column[Int]("rank")

    def gamesWon = column[Long]("games_won")

    def gamesTied = column[Long]("games_tied")

    def roundsWon = column[Long]("rounds_won")

    def kills = column[Long]("kills")

    def deaths = column[Long]("deaths")

    def assists = column[Long]("assists")

    def entryKills = column[Long]("entry_kills")

    def damageDone = column[Long]("damage_done")

    def mvps = column[Long]("mvps")

    def rating = column[Float]("rating")

    def plants = column[Long]("plants")

    def defuses = column[Long]("defuses")

    def clutchesWon = column[Long]("clutches_won")

    def created = column[DateTime]("created")

    def changed = column[DateTime]("changed")

    def * = (
      id,
      steamId,
      steam64bit,
      name,
      playedGames,
      playedRounds,
      gamesWon,
      gamesTied,
      roundsWon,
      kills,
      deaths,
      assists,
      entryKills,
      damageDone,
      mvps,
      rating,
      plants,
      defuses,
      clutchesWon,
      created,
      changed
      ) <>(PlayerProfile.tupled, PlayerProfile.unapply)
  }

}

@ImplementedBy(classOf[PlayerProfileDaoImpl])
trait PlayerProfileDao {
  def findWithSteamId(steamId: String): Future[Option[PlayerProfile]]
  def insertPlayerProfile(playerProfile: PlayerProfile): Future[PlayerProfile]
  def updatePlayerProfile(playerProfile: PlayerProfile): Future[Int]
}

class PlayerProfileDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends  PlayerProfileDaoComponent with PlayerProfileDao with  HasDatabaseConfigProvider[JdbcProfile] {

  val playerProfiles = TableQuery[PlayerProfiles]

  def findWithSteamId(steamId: String) = {
    val rs = db.run(playerProfiles.filter(_.steamId === steamId).result.headOption)
    rs
  }

  def insertPlayerProfile(playerProfile: PlayerProfile): Future[PlayerProfile] = {
    val insertQuery = playerProfiles returning playerProfiles.map(_.id) into ((playerProfile, id) => playerProfile.copy(id = id))
    val action = insertQuery += playerProfile
    db.run(action)
  }

  def updatePlayerProfile(playerProfile: PlayerProfile) = {
    val q = for { profile <- playerProfiles if profile.steamId === playerProfile.steamId } yield (
      profile.playedGames,
      profile.playedRounds,
      profile.gamesWon,
      profile.gamesTied,
      profile.roundsWon,
      profile.kills,
      profile.deaths,
      profile.assists,
      profile.entryKills,
      profile.damageDone,
      profile.mvps,
      profile.rating,
      profile.plants,
      profile.defuses,
      profile.clutchesWon,
      profile.changed,
      profile.name
      )
    val updateAction = q.update(
      playerProfile.playedGames,
      playerProfile.playedRounds,
      playerProfile.gamesWon,
      playerProfile.gamesTied,
      playerProfile.roundsWon,
      playerProfile.kills,
      playerProfile.deaths,
      playerProfile.assists,
      playerProfile.entryKills,
      playerProfile.damageDone,
      playerProfile.mvps,
      playerProfile.rating,
      playerProfile.plants,
      playerProfile.defuses,
      playerProfile.clutchesWon,
      playerProfile.changed,
      playerProfile.name
    )

    db.run(updateAction)
  }
}

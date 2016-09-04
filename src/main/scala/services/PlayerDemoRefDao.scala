package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import databasemodels.{PlayerDemoRef}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._
import scala.concurrent.Future

trait PlayerDemoRefComponent {

  class PlayerDemoRefs(tag: Tag) extends Table[PlayerDemoRef](tag, "player_demo_ref") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def steamId = column[String]("steam_id")

    def demoId = column[Long]("demo_id")

    def * = (
      id,
      steamId,
      demoId) <>(PlayerDemoRef.tupled, PlayerDemoRef.unapply)
  }

}

@ImplementedBy(classOf[PlayerDemoRefDaoImpl])
trait PlayerDemoRefDao {
  def insertRef(playerDemoRef: PlayerDemoRef): Future[Unit]
}

class PlayerDemoRefDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends PlayerDemoRefComponent with PlayerDemoRefDao with HasDatabaseConfigProvider[JdbcProfile]{

  val playerDemoRefs = TableQuery[PlayerDemoRefs]

  def insertRef(playerDemoRef: PlayerDemoRef) = {
    db.run(playerDemoRefs += playerDemoRef).map(_ => ())
  }
}

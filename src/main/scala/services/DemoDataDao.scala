package services

import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.ImplementedBy
import databasemodels.DemoData
import org.joda.time.DateTime
import play.api.Play
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._
import scala.concurrent.Future
import com.google.inject.ImplementedBy

trait DemoDataDaoComponent {

  class DemoDatas(tag: Tag) extends Table[DemoData](tag, "demo") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def matchId = column[String]("match_id")

    def shareCode = column[String]("share_code")

    def demoUrl = column[String]("demo_url")

    def matchDate = column[DateTime]("match_date")

    def matchData = column[JsValue]("match_data")

    def map = column[String]("map")

    def team1Score = column[Int]("team1_score")

    def team2Score = column[Int]("team2_score")

    def created = column[DateTime]("created")

    def * = (
      id,
      matchId,
      shareCode,
      demoUrl,
      matchDate,
      matchData,
      map,
      team1Score,
      team2Score,
      created) <>(DemoData.tupled, DemoData.unapply)
  }
}

trait DemoDataDao {
  def insertDemo(demoData: DemoData): Future[DemoData]
}

class DemoDataDaoImpl extends DemoDataDao with DemoDataDaoComponent {
  import driver.api._
  val db = Database.forConfig("slick.dbs.default.db")
  val demoDatas = TableQuery[DemoDatas]

  def insertDemo(demoData: DemoData): Future[DemoData] = {
    val insertQuery = demoDatas returning demoDatas.map(_.id) into ((demoData, id) => demoData.copy(id = id))
    val action = insertQuery += demoData
    db.run(action)
  }
}
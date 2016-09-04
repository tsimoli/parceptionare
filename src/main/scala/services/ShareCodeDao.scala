package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.ShareCode
import org.joda.time.DateTime
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import slick.lifted.Tag
import util.MyPostgresDriver.api._
import com.github.tototoshi.slick.PostgresJodaSupport._

import scala.concurrent.Future

trait ShareCodeComponent {

  class ShareCodes(tag: Tag) extends Table[ShareCode](tag, "sharecode") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def shareCode = column[String]("share_code")

    def matchId = column[String]("match_id")

    def outcomeId = column[String]("outcome_id")

    def token = column[String]("token")

    def status = column[String]("status")

    def created = column[DateTime]("created")

    def * = (
      id,
      shareCode,
      matchId,
      outcomeId,
      token,
      status,
      created) <>(ShareCode.tupled, ShareCode.unapply)
  }
}

@ImplementedBy(classOf[ShareCodeDaoImpl])
trait ShareCodeDao {
  def findShareCode(shareCode: String): Future[Option[ShareCode]]
  def insertShareCode(shareCode: ShareCode): Future[Unit]
  def updateShareCodeStatus(shareCodeString: String, status: String): Future[Int]
}

class ShareCodeDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends ShareCodeDao with ShareCodeComponent with HasDatabaseConfigProvider[JdbcProfile]{

  val shareCodes = TableQuery[ShareCodes]

  def findShareCode(shareCode: String) = {
    db.run(shareCodes.filter(_.shareCode === shareCode).result.headOption)
  }

  def insertShareCode(shareCode: ShareCode) = {
    db.run(shareCodes += shareCode).map(_ => ())
  }

  def updateShareCodeStatus(shareCodeString: String, status: String) = {
    val q = for { shareCodeRow <- shareCodes if shareCodeRow.shareCode === shareCodeString } yield shareCodeRow.status
    val updateAction = q.update(status)
    db.run(updateAction)
  }
}
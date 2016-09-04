package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import models.ShareCode
import org.joda.time.DateTime
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import scala.concurrent._
import com.github.tototoshi.slick.PostgresJodaSupport._

trait ParseQueueComponent {

  class ParseQueues(tag: Tag) extends Table[ParseQueue](tag, "parse_queue") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def shareCode = column[String]("share_code")

    def matchId = column[String]("match_id")

    def url = column[String]("url")

    def matchDuration = column[Long]("match_duration")

    def status = column[String]("status")

    def matchDate = column[DateTime]("match_date")

    def created = column[DateTime]("created")

    def * = (
      id,
      shareCode,
      matchId,
      url,
      matchDuration,
      status,
      matchDate,
      created) <>(ParseQueue.tupled, ParseQueue.unapply)
  }
}

case class ParseQueue(id: Long, matchId: String, shareCode: String, url: String, matchDuration: Long, status: String, matchDate: DateTime, created: DateTime)

@ImplementedBy(classOf[ParseQueueDaoImpl])
trait ParseQueueDao {
  def queryQueue: Future[Seq[((String, String, String, Long, Long))]]
  def updateQueueItemStatus(shareCodeString: String, status: String): Future[Int]
  def deleteByShareCode(shareCode: String)
}

class ParseQueueDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends ParseQueueDao with HasDatabaseConfigProvider[JdbcProfile] with ParseQueueComponent {

  val parseQueues = TableQuery[ParseQueues]

  def queryQueue: Future[Seq[((String, String, String, Long, Long))]] = {
   val result =  db.run(sql"""update parse_queue set status='running'
          where share_code in ( select share_code from parse_queue where status='new' order by
          created asc limit 1 for update ) returning match_id, share_code, url, match_duration, match_date""".as[(String, String, String, Long, Long)])
    result
  }

  def updateQueueItemStatus(shareCodeString: String, status: String) = {
    val q = for { shareCodeRow <- parseQueues if shareCodeRow.shareCode === shareCodeString } yield shareCodeRow.status
    val updateAction = q.update(status)
    db.run(updateAction)
  }

  def deleteByShareCode(shareCode: String) = {
    val action = parseQueues.filter(_.shareCode === shareCode).delete
    db.run(action)
  }
}
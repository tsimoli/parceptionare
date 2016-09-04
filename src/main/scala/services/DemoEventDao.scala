package services

import java.util.UUID
import com.websudos.phantom.dsl._
import models.DemoEvent
import scala.concurrent.Future

class DemoEvents extends CassandraTable[DemoEvents, DemoEvent] {

  object id extends UUIDColumn(this) with PartitionKey[UUID]

  object eventType extends StringColumn(this)

  object steamId extends StringColumn(this)

  object map extends StringColumn(this)

  object changed extends DateTimeColumn(this)

  object created extends DateTimeColumn(this)

  def fromRow(row: Row): DemoEvent = {
    DemoEvent(
      eventType(row),
      steamId(row),
      created(row),
      changed(row)
    )
  }
}

object DemoEventDao extends DemoEvents with BasicConnector {
//  def insertNewRecord(demoEvent: DemoEvent):Future[ResultSet] = {
//    insert.value(_.eventType, demoEvent.eventType)
//      .future()
//  }
}


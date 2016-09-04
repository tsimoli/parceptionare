package models

import scala.collection.mutable.ListBuffer
import com.google.protobuf.CodedInputStream

class SendTable(reader: CodedInputStream) {
  var properties = ListBuffer[SendTableProperty]()
  var name: String = ""
  var isEnd: Boolean = false

  val dataTable = new DataTable();

  dataTable.parse(reader).foreach { prop =>
    val sendTableProp = new SendTableProperty()
    sendTableProp.dataTableName = prop.dtName
    sendTableProp.highValue = prop.highValue
    sendTableProp.lowValue = prop.lowValue
    sendTableProp.name = prop.varName
    sendTableProp.numberOfBits = prop.numBits
    sendTableProp.numberOfElements = prop.numElements
    sendTableProp.priority = prop.priority
    sendTableProp.rawFlags = prop.flags
    sendTableProp.rawType = prop.propType

    properties += sendTableProp

  }

  this.name = dataTable.netTableName;
  this.isEnd = dataTable.isEndBoolean;

}
package dp

import parsers.DemoParser
import com.google.protobuf.CodedInputStream
import scala.collection.mutable.ListBuffer
import dp.handler.GameEventHandler

class GameEvent {
  var eventName = ""
  var eventId = 0
  var keys = ListBuffer[Any]()

  def parse(reader: CodedInputStream, parser: DemoParser) = {
    while (!reader.isAtEnd()) {
      var desc = reader.readRawVarint32()
      var wireType = desc & 7
      var fieldNum = desc >> 3

      if ((wireType == 2) && (fieldNum == 1)) {
        val len = reader.readRawVarint32()
        eventName = new String(reader.readRawBytes(len))
      } else if ((wireType == 0) && (fieldNum == 2)) {
        eventId = reader.readRawVarint32()
      } else if ((wireType == 2) && (fieldNum == 3)) {
        reader.readRawVarint32()
        desc = reader.readRawVarint32()
        wireType = desc & 7
        fieldNum = desc >> 3
        val typeMember = reader.readRawVarint32()
        desc = reader.readRawVarint32()
        wireType = desc & 7
        fieldNum = desc >> 3

        typeMember match {
          case 1         => if (wireType == 2) keys += new String(reader.readRawBytes(reader.readRawVarint32()))
          case 2         => if (wireType == 5) keys += reader.readFloat()
          case 3 | 4 | 5 => if (wireType == 0) keys += reader.readRawVarint32().toInt
          case 6         => if (wireType == 0) keys += (reader.readRawVarint32().toInt != 0)
        }
      }
    }
    GameEventHandler.apply(this, parser)
  }
}
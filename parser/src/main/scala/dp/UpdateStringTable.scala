package dp

import com.google.protobuf.CodedInputStream
import parsers.DemoParser
import dp.handler.UpdateStringDataHandler
import stream.BitStream
import scala.util.control.Breaks._

class UpdateStringTable {
  var tableId: Int = 0
  var numChangedEntries = 0

  def parse(reader: CodedInputStream, parser: DemoParser) = {
    breakable {
      while (!reader.isAtEnd()) {
        val desc = reader.readRawVarint32()
        val wireType = desc & 7
        val fieldNum = desc >> 3

        if ((wireType == 2) && (fieldNum == 3)) {
          val length = reader.readRawVarint32()
          UpdateStringDataHandler.apply(this, new BitStream(reader.readRawBytes(length)), parser)
          break
        }

        val value = reader.readRawVarint32()
        fieldNum match {
          case 1 => tableId = value
          case 2 => numChangedEntries = value
          case _ =>
        }

      }
    }
  }
}
package dp

import com.google.protobuf.CodedInputStream
import dp.handler.PacketEntitesHandler
import parsers.DemoParser
import stream.BitStream
import scala.util.control.Breaks._

class PacketEntities() {
  var maxEntries: Int = 0
  var UpdatedEntries: Int = 0
  var IsDelta: Boolean = false
  var UpdateBaseline: Boolean = false
  var Baseline: Int = 0
  var DeltaFrom: Int = 0

  def parse(reader: CodedInputStream, parser: DemoParser) = {
    breakable {
      while (!reader.isAtEnd()) {

        if(parser.currentTick == 85726) {
          val i = 0
        }
//        println("Tick: " + parser.currentTick)
        val desc = reader.readRawVarint32()
        val wireType = desc & 7
        val fieldNum = desc >> 3

        if ((fieldNum == 7) && (wireType == 2)) {
          val length = reader.readRawVarint32()
          PacketEntitesHandler.apply(this, new BitStream(reader.readRawBytes(length)), parser)
          break
        }

        val value = reader.readRawVarint32()
        fieldNum match {
          case 1 => maxEntries = value
          case 2 => UpdatedEntries = value
          case 3 => if (value != 0) IsDelta = true
          case 4 => if (value != 0) UpdateBaseline = true
          case 5 => Baseline = value
          case 6 => DeltaFrom = value
        }

      }
    }
  }
}
package dp

import parsers.DemoParser
import java.math.BigInteger
import scala.util.control.Breaks._
import com.google.protobuf.CodedInputStream
import stream.BitStream

class CreateStringTable() {
  var name: String = ""
  var maxEntries: Int = 0
  var numEntries: Int = 0
  var userDataFixedSize: Int = 0
  var isUserDataFixedSize = userDataFixedSize != 0
  var userDataSize: Int = 0
  var userDataSizeBits: Int = 0
  var flags: Int = 0

  def parse(reader: CodedInputStream, demoParser: DemoParser) = {
    breakable {
      while (!reader.isAtEnd()) {
        val desc = reader.readRawVarint32()
        val wireType = desc & 7
        val fieldNum = desc >> 3
        if (wireType == 2) {
          if (fieldNum == 1) {
            val len = reader.readRawVarint32()
            name = new String(reader.readRawBytes(len))
          } else if (fieldNum == 8) {
            val len = reader.readRawVarint32()
            val readBytes = reader.readRawBytes(len)
            CreateStringTableUserInfoHandler.apply(this, new BitStream(readBytes), demoParser)
            break
          }
        }

        if (fieldNum != 1) {

          val value = reader.readRawVarint32()

          fieldNum match {
            case 2 =>
              maxEntries = value
            case 3 => numEntries = value
            case 4 => if (value != 0) isUserDataFixedSize = true
            case 5 => userDataSize = value
            case 6 => userDataSizeBits = value
            case 7 => flags = value
            case _ => println("Fail")
          }
        }
      }
    }

  }
}

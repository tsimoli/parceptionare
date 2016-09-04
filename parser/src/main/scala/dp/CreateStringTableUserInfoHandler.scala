package dp

import parsers.DemoParser
import stream.BitStream

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

object CreateStringTableUserInfoHandler {
  def apply(table: CreateStringTable, reader: BitStream, parser: DemoParser) = {
    if (reader.readBit()) {
      println("Fail bit is here")
    }

    var nTemp = table.maxEntries
    var nEntryBits = 0
    while ({ nTemp >>= 1; nTemp != 0 }) {
      nEntryBits += 1
    }

    val history = ListBuffer[String]()
    var lastEntry = -1

    breakable {
      for (i <- 0 until table.numEntries) {

        var entryIndex = lastEntry + 1

        if (!reader.readBit()) {
          entryIndex = reader.readNumericBits(nEntryBits)
        }

        lastEntry = entryIndex

        var entry = ""

        if (reader.readBit()) {
          val subStrinCheck = reader.readBit()
          if (subStrinCheck) {
            val index = reader.readNumericBits(5)
            val bytesToCopy = reader.readNumericBits(5)

            entry = history(index).substring(0, bytesToCopy)

            entry += reader.readString(1024)

          } else {
            entry = reader.readString(1024)
          }
        }

        if (entry == null) {
          entry = ""
        }

        if (history.size > 31) history.remove(0)

        history += entry

        var userData: Array[Byte] = Array.empty[Byte]

        if (reader.readBit()) {
          if (table.isUserDataFixedSize) {
            userData = reader.readBits(table.userDataSizeBits)
          } else {
            userData = reader.readBits(reader.readNumericBits(14) * 8)
          }
        }

        if (userData.length == 0) {
          break
        }

        if (table.name == "userinfo") {
          // no need for now
        } else if (table.name == "instancebaseline") {
          val classId = entry.toInt
          parser.instanceBaseline(classId) = userData
        }
      }
    }

    parser.stringTables += table
  }
}
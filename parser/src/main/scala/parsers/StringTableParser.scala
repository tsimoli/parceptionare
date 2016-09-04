package parsers

import java.util.BitSet
import com.google.protobuf.CodedInputStream
import stream.BitStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import models.PlayerInfoParser

class StringTableparser {
  def parsePacket(reader: BitStream, parser: DemoParser) = {

    val numTables: Int = reader.readNumericBits(8)

    for (i <- 0 to numTables) {
      val tableName = StringParser.readDataTableString(reader)
      parseStringTable(reader, tableName, parser)
    }
  }

  def parseStringTable(reader: BitStream, tableName: String, parser: DemoParser) = {
    val numStrings = reader.readNumericBits(16)

    if (tableName == "modelprecache") {
          parser.modelprecache.clear()
      }
    
    for (i <- 0 until numStrings) {
      val stringName = StringParser.readDataTableString(reader)

      if (stringName.length() >= 100) {
        println("Too fucking long!")
      }

      if (reader.readBit()) {
        val userDataSize = reader.readNumericBits(16)

        val data = reader.readBits(userDataSize * 8)

        if (tableName == "userinfo") {
          val player = PlayerInfoParser.parserFrom(CodedInputStream.newInstance(data), stringName.toInt + 1)
          if (player.steamID != "BOT") {
//            println("userId: " + player.userId)
//            println("name: " + player.name)
//            println("index: " + player.entityId)
//            println("steamId: " + player.steamID)
//            println("entityId: " + player.entityId)
//            println("==============================")
            parser.players(player.userId) = Some(player)
          }
        } else if (tableName == "instancebaseline") {
          val classId = stringName.toInt
          parser.instanceBaseline(classId) = data
        } else if (tableName == "modelprecache") {
          parser.modelprecache = parser.modelprecache += stringName
        }
      }
    }

    if (reader.readBit()) {
      val numStrings = reader.readBits(8)(0) + reader.readBits(8)(0)
      for (i <- 0 to numStrings) {
        val oo = StringParser.readDataTableString(reader)
        if (reader.readBit()) {
          val userDataSize = reader.readBits(8)(0) + reader.readBits(8)(0)
          reader.readBits(userDataSize * 8)
        } else {}
      }
    }
  }

}
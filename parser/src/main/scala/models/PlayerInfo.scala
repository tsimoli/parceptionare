package models

import com.google.protobuf.CodedInputStream
import parsers.main.Enums.Equipment
import stream.BitStream
import java.nio.ByteBuffer
import scala.collection.mutable.HashMap
object PlayerInfoParser {
  def parserFrom(reader: CodedInputStream, entityId: Int) = {
    val version = ByteBuffer.wrap(reader.readRawBytes(8)).getLong
    val steamId64bit = ByteBuffer.wrap(reader.readRawBytes(8)).getLong // steamid 64 bit
    val name = new String(reader.readRawBytes(128)).trim
    val userId = ByteBuffer.wrap(reader.readRawBytes(4)).getInt // entityid
    val steamId = new String(reader.readRawBytes(33)).split("\u0000")(0) // steamId
    val friendsId = reader.readRawLittleEndian32()
    val friendsName = new String(reader.readRawBytes(128)).trim
    val isFakePlayer = reader.readBool()
    val isHLTV = reader.readBool()
    val custoFiles0 = reader.readRawLittleEndian32()
    val custoFiles1 = reader.readRawLittleEndian32()
    val custoFiles2 = reader.readRawLittleEndian32()
    val custoFiles3 = reader.readRawLittleEndian32()
    val filesDownloaded = reader.readRawByte()

    new Player(
      name,
      steamId,
      steamId64bit.toString,
      userId,
      0.0.toFloat,
      0.0.toFloat,
      0.0.toFloat,
      entityId,
      0,
      0,
      0.0.toFloat,
      0.0.toFloat,
      0,
      -1,
      HashMap[Int, Equipment](),
      List[Int](),
      false,
      false,
      -1,
      Array[Int](),
      Some(AdditionalInformation("", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
      0,
      0,
      0,
      "dead",
      entityId,
      "",
    false)
  }
}
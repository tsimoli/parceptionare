package parsers

import java.nio.ByteBuffer
import scala.collection.immutable.BitSet
import models.Header
import com.google.protobuf.CodedInputStream

object HeaderParser {
  def parseHeader(reader: CodedInputStream): Header = {
    val fileStamp = new String(reader.readRawBytes(8)).trim
    val protocol = reader.readRawLittleEndian32()
    val networkProtocol = reader.readRawLittleEndian32()
    val serverName = new String(reader.readRawBytes(260)).trim

    val clientName = new String(reader.readRawBytes(260)).trim
    val mapName = new String(reader.readRawBytes(260)).trim
    val gameDirectory = new String(reader.readRawBytes(260)).trim

    val playbackTime = reader.readFloat()

    val playbackTicks = reader.readRawLittleEndian32()
    val playbackFrames = reader.readRawLittleEndian32()
    val signonLength = reader.readRawLittleEndian32()

    Header(fileStamp, protocol, networkProtocol, serverName, clientName, mapName, gameDirectory, playbackTime, playbackTicks, playbackFrames, signonLength)
  }
}
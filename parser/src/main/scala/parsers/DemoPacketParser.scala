package parsers

import dp.CreateStringTable
import messages.Cstrike15Usermessages
import model.RankUpdate
import models.Events.RankUpdateEvent
import models.NetTick
import com.google.protobuf.{InvalidProtocolBufferException, CodedInputStream}
import dp.PacketEntities
import dp.GameEvent
import dp.GameEventList
import dp.UpdateStringTable
import parsers.main.Enums._
import stream.BitStream
import scala.collection.JavaConversions._

object DemoPacketParser {
  val VALVE_MAGIC_NUMBER = 76561197960265728L;
  def parsePacket(reader: CodedInputStream, demoParser: DemoParser) = {
    while (!reader.isAtEnd()) {
      val cmd = reader.readRawVarint32()
      val length = reader.readRawVarint32()
      val bytes = reader.readRawBytes(length)
      cmd match {
        case NET_Messages.net_Tick           => {
          new NetTick().parse(CodedInputStream.newInstance(bytes))
        }
        case SVC_Messages.svc_PacketEntities => {
          new PacketEntities().parse(CodedInputStream.newInstance(bytes), demoParser)
        }
        case SVC_Messages.svc_EncryptedData  => // Key for this and we can rule the world
        case SVC_Messages.svc_GameEventList  =>{
          new GameEventList().parse(CodedInputStream.newInstance(bytes), demoParser)
        } 
        case SVC_Messages.svc_GameEvent      => {
          new GameEvent().parse(CodedInputStream.newInstance(bytes), demoParser)
        }
        case SVC_Messages.svc_CreateStringTable => {
          new CreateStringTable().parse(CodedInputStream.newInstance(bytes), demoParser)
        }
        case SVC_Messages.svc_UpdateStringTable => {
          new UpdateStringTable().parse(CodedInputStream.newInstance(bytes), demoParser)
        }
        case SVC_Messages.svc_UserMessage => {
          val data = CodedInputStream.newInstance(bytes)
          // drop 5 first bytes to parse to correct protobuf
          try {
            val rankUpdate = Cstrike15Usermessages.CCSUsrMsg_ServerRankUpdate.parseFrom(bytes.drop(5))
            val rankUpdateEvents = rankUpdate.getRankUpdateList.map(rankUpdateProto => RankUpdate(rankUpdateProto.getAccountId + VALVE_MAGIC_NUMBER, rankUpdateProto.getNumWins, rankUpdateProto.getRankNew, rankUpdateProto.getRankOld)).toList
            if(rankUpdateEvents.size > 1)
              demoParser.raiseRankUpdate(RankUpdateEvent(rankUpdateEvents))
          } catch {
            case e: InvalidProtocolBufferException => {
              // Ignore
            }
          }
        }
        case _ => {

        }
      }
    }
  }
}
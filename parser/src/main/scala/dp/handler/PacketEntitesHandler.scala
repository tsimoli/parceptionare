package dp.handler

import dp.PacketEntities
import models.Entity
import parsers.DemoParser
import stream.BitStream


object PacketEntitesHandler {

  def apply(packetEntities: PacketEntities, reader: BitStream, parser: DemoParser) = {
    var currentEntity = -1

    for (i <- 0 until packetEntities.UpdatedEntries) {

      // First read which entity is updated
      currentEntity += 1 + reader.readUBitInt()

      // Find out whether we should create, destroy or update it. 
      // Leave flag
      if (!reader.readBit()) {
        // enter flag
        if (reader.readBit()) {
          // create it
          val e = readEnterPVS(reader, currentEntity, parser)
          parser.entities(currentEntity) = e
          e.applyUpdate(reader)
        } else {
          // preserve / update
          val e = parser.entities(currentEntity)
          e.applyUpdate(reader)
        }
      } else {
        // leave / destroy
        parser.entities(currentEntity).leave()
        parser.entities(currentEntity) = null
        if (reader.readBit()) {}
      }
    }
  }

  /**
   * Reads an update that occures when a new edict enters the PVS (potentially visible system)
   */
  def readEnterPVS(reader: BitStream, id: Int, parser: DemoParser) = {

    val bits = parser.dataTableParser.classBits

    // What kind of entity?
    val serverClassID = reader.readNumericBits(bits)

    // So find the correct server class
    val entityClass = parser.dataTableParser.serverClasses(serverClassID)

    // Never used anywhere.
    reader.readNumericBits(10) // entity serial
    
    val newEntity = new Entity(id, entityClass)

    entityClass.announceNewEntity(newEntity)
    
    newEntity.applyUpdate(new BitStream(parser.instanceBaseline(serverClassID)))
     
    newEntity
  }
}
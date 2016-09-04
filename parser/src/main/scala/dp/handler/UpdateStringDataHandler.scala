package dp.handler

import dp.UpdateStringTable
import parsers.DemoParser
import stream.BitStream
import dp.CreateStringTableUserInfoHandler

object UpdateStringDataHandler {
  def apply(update: UpdateStringTable, reader: BitStream, parser: DemoParser) = {
    val create = parser.stringTables(update.tableId)

    if (create.name == "userinfo" || create.name == "modelprecache" || create.name == "instancebaseline") {
      create.numEntries = update.numChangedEntries
      CreateStringTableUserInfoHandler.apply(create, reader, parser)
    }
  }
}
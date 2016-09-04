package dt

import com.google.protobuf.CodedInputStream
import models.{SendTable, _}
import parsers.StringParser
import parsers.main.Enums.{SendPropertyType, SVC_Messages}
import stream.BitStream

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class DataTableParser {
  var dataTables = ListBuffer[SendTable]()
  var serverClasses = ListBuffer[ServerClass]()
  var currentExcludes = ListBuffer[ExcludeEntry]()
  var currentBaseClasses = ListBuffer[ServerClass]()

  def classBits() = {
    Math.ceil(Math.log(serverClasses.size) / Math.log(2)).toInt
  }

  def parsePacket(reader: BitStream) = {
    breakable {
      while (true) {
        val sendTableType = (SVC_Messages)(reader.readVarInt())
        val size = reader.readVarInt()
        val sendTable = new SendTable(CodedInputStream.newInstance(reader.readBits((size * 8))))
        if (sendTable.isEnd) {
          break
        }
        dataTables += sendTable
      }
    }

    val serverClassCount = reader.readNumericBits(16)

    for (i <- 0 until (serverClassCount)) {
      val serverClass = new ServerClass()
      serverClass.classID = reader.readNumericBits(16)

      if (serverClass.classID > serverClassCount) {
        println("Should fail")
      }
      serverClass.name = StringParser.readDataTableString(reader)
      serverClass.dtName = StringParser.readDataTableString(reader)

      serverClass.dataTableId = dataTables.indexWhere { dataTable =>
        dataTable.name == serverClass.dtName
      }
      serverClasses = serverClasses += serverClass
    }
    for (i <- 0 until serverClassCount) {
      flattenDataTable(i)
    }
  }

  def flattenDataTable(serverClassIndex: Int) = {
    val table = dataTables(serverClasses(serverClassIndex).dataTableId)

    currentExcludes.clear()
    currentBaseClasses = ListBuffer[ServerClass]()
    getherExcludesAndBaseClasses(table, true)
    serverClasses(serverClassIndex).baseClasses = currentBaseClasses

    gatherProps(table, serverClassIndex, "")
    val flattenedProps = serverClasses(serverClassIndex).flattenedProps

    val prioritiesTemp = ListBuffer[Int]()
    var priorities = ListBuffer[Int]()
    prioritiesTemp += 64
    prioritiesTemp ++= flattenedProps.map(flatProp => flatProp.prop.priority).distinct
    priorities = prioritiesTemp.sorted
    var start = 0

    for (i <- 0 until priorities.size) {
      val priority = priorities(i)
      breakable {
        while (true) {
          var currentProp = start
          breakable {
            while (currentProp < flattenedProps.size) {
              val prop = flattenedProps(currentProp).prop
              if (prop.priority == priority || (priority == 64 && (prop.rawFlags & SendPropertyFlags.ChangesOften) == SendPropertyFlags.ChangesOften)) {
                if (start != currentProp) {
                  val temp = flattenedProps(start)
                  flattenedProps(start) = flattenedProps(currentProp)
                  flattenedProps(currentProp) = temp

                }
                start += 1
                break
              }
              currentProp += 1
            }
          }
          if (currentProp == flattenedProps.size) break
        }
      }

    }

  }

  def getherExcludesAndBaseClasses(table: SendTable, collectBaseClasses: Boolean): Unit = {
    currentExcludes ++= table.properties.filter(prop => ((prop.rawFlags & SendPropertyFlags.Exclude) == SendPropertyFlags.Exclude)).map(mapped =>
      new ExcludeEntry(mapped.name, mapped.dataTableName, table.name))

    table.properties.filter(prop => prop.rawType == SendPropertyType.dataTable).foreach { prop =>
      if (collectBaseClasses && prop.name == "baseclass") {
        getherExcludesAndBaseClasses(getTableByName(prop.dataTableName), true)
        currentBaseClasses += findByDtName(prop.dataTableName)
      } else {
        getherExcludesAndBaseClasses(getTableByName(prop.dataTableName), false)
      }
    }

  }

  def getTableByName(className: String) = {
    dataTables.find { table =>
      table.name == className
    }.getOrElse(null)
  }
  def findByName(name: String) = {
    serverClasses.find(_.name == name).getOrElse(null)
  }
  
  def findByDtName(dtName: String) = {
    serverClasses.find(_.dtName == dtName).getOrElse(null)
  }

  def gatherProps(table: SendTable, serverClassIndex: Int, prefix: String) = {
    val tmpFlattenedProps = ListBuffer[FlattenedPropEntry]()
    gatherPropsIterateProps(table, serverClassIndex, tmpFlattenedProps, prefix)
    val flattenedProps = serverClasses(serverClassIndex).flattenedProps
    flattenedProps ++= tmpFlattenedProps
    val i = 0
  }

  def gatherPropsIterateProps(table: SendTable, serverClassIndex: Int, flattenedProps: ListBuffer[FlattenedPropEntry], prefix: String): Unit = {

    for (i <- 0 until table.properties.length) {
      breakable {
        val prop = table.properties(i)
        if ((prop.rawFlags & SendPropertyFlags.InsideArray) == SendPropertyFlags.InsideArray ||
          (prop.rawFlags & SendPropertyFlags.Exclude) == SendPropertyFlags.Exclude ||
          isPropExcluded(table, prop)) {
          break
        }

        if (prop.rawType == SendPropertyType.dataTable) {

          val subTable = getTableByName(prop.dataTableName)
          if ((prop.rawFlags & SendPropertyFlags.Collapsible) == SendPropertyFlags.Collapsible) {
            gatherPropsIterateProps(subTable, serverClassIndex, flattenedProps, prefix)
          } else {
            val prefixed = if (prop.name.length() > 0) {
              (prop.name + ".")
            } else ""
            val nfix = prefix + prefixed
            gatherProps(subTable, serverClassIndex, nfix)
          }
        } else {
          if (prop.rawType == SendPropertyType.array) {
            flattenedProps += new FlattenedPropEntry(prefix + prop.name, prop, table.properties(i - 1))
          } else {
            flattenedProps += new FlattenedPropEntry(prefix + prop.name, prop, null)
          }
        }
      }
    }
  }

  def isPropExcluded(table: SendTable, prop: SendTableProperty) = {
    val ret = currentExcludes.exists { exclude => table.name == exclude.dtName && prop.name == exclude.varName }
    if (currentExcludes.size > 0) {
      val ii = 0
    }
    ret
  }

}
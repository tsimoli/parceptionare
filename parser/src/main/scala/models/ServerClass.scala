package models

import scala.collection.mutable.ListBuffer
import rx.lang.scala.Subject

class ServerClass() {
  var classID: Int = 0
  var dataTableId: Int = 0
  var name: String = ""
  var dtName: String = ""

  var flattenedProps = ListBuffer[FlattenedPropEntry]()

  var baseClasses = ListBuffer[ServerClass]()
  
  val newEntitySubject = Subject[NewEntityArgs]()
  
  def announceNewEntity(e: Entity) = {
    newEntitySubject.onNext(NewEntityArgs(e, this))
  }
}

case class NewEntityArgs(entity: Entity, serverClass: ServerClass)

case class FlattenedPropEntry(var propertyName: String, var prop: SendTableProperty, var arrayElementProp: SendTableProperty) {
}

class ExcludeEntry(var varName: String, var dtName: String, var excludingDT: String) {}
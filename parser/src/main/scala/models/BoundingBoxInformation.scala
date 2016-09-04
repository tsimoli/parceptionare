package models

class BoundingBoxInformation {
  var index = 0
  var min = Vector(0, 0, 0)
  var max = Vector(0, 0, 0)

  def contains(point: Vector): Boolean = {
    point.x >= min.x && point.x <= max.x &&
      point.y >= min.y && point.y <= max.y &&
      point.y >= min.z && point.z <= max.z
  }
}
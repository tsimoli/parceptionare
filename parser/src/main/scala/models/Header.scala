package models

case class Header(
  fileStamp: String,
  protocol: Int,
  networkProtocol: Int,
  serverName: String,
  clientName: String,
  mapName: String,
  gameDirectory: String,
  playBackTime: Float,
  playbackTicks: Int,
  playbackFrames: Int,
  signonLength: Int
  )

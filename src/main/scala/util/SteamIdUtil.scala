package util

/**
  * Created by Turo on 3.11.2015.
  */
object SteamIdUtil {
  def transformSteamIdTo64bit(steamId: String): Long = {
    val authServerAndId = steamId.splitAt(8)
    val steamIdArray = authServerAndId._2.split(":")
    if (steamIdArray.size == 2) {
      (steamIdArray(1).toLong * 2) + (steamIdArray(0).toLong + 76561197960265728L)
    }
    else 0
  }
}

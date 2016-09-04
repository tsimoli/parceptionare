package models

import org.joda.time.DateTime

case class DemoEvent(eventType: String, steamId: String, created: DateTime, changed: DateTime)

// Rank up or down events for statistic (how many this week got rank up or down)
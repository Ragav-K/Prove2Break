package com.prove2break.apprestriction

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Detects the current foreground app via [UsageStatsManager]'s event log.
 *
 * Real limitation (documented per APP-RESTRICTION.md / PERMISSIONS.md): this
 * is a polling read of a system log, not a push notification — there is
 * inherent latency between an app actually coming to the foreground and this
 * detector observing it, bounded by how often [AppRestrictionMonitor] calls
 * this. A determined/fast user can see a blocked app's UI for a brief moment
 * before the interstitial appears. An AccessibilityService would close that
 * gap but was deliberately not adopted for MVP (see APP-RESTRICTION.md).
 */
object ForegroundAppDetector {

    /** Looks back [lookbackMillis] from now and returns the most recent MOVE_TO_FOREGROUND package, if any. */
    fun getForegroundPackage(context: Context, lookbackMillis: Long = 10_000L): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - lookbackMillis, now)

        var lastForegroundPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                lastForegroundPackage = event.packageName
            }
        }
        return lastForegroundPackage
    }
}

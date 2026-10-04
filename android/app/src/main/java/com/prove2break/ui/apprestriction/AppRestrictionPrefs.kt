package com.prove2break.ui.apprestriction

import android.content.Context

/**
 * Tiny SharedPreferences-backed flag for "has the honest-limitations
 * disclosure been shown once" (APP-RESTRICTION.md's Honest Disclosure). Kept
 * out of Room deliberately — it's a one-off UI flag, not durable app data.
 */
object AppRestrictionPrefs {
    private const val PREFS_NAME = "app_restriction_prefs"
    private const val KEY_DISCLOSURE_SEEN = "honest_limitations_disclosure_seen"

    fun hasSeenDisclosure(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISCLOSURE_SEEN, false)
    }

    fun markDisclosureSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISCLOSURE_SEEN, true)
            .apply()
    }
}

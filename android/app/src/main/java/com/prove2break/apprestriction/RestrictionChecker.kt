package com.prove2break.apprestriction

import com.prove2break.domain.model.EmergencyUnlock
import com.prove2break.domain.model.FocusSessionStatus

/**
 * The single source of truth for "should this foreground app be blocked right
 * now" — deliberately a pure function over its inputs (no Context, no clock
 * reads of its own) so it can be unit tested without Robolectric/instrumentation.
 * Called by the foreground-detection loop in [AppRestrictionMonitor].
 */
object RestrictionChecker {

    /**
     * @param foregroundPackage the package currently in the foreground, or null if unknown/undetectable.
     * @param selectedBlockedPackages the student's manually chosen restricted packages.
     * @param sessionStatus the active focus session's status, or null if there is no active session.
     * @param activeEmergencyUnlocks unlocks for the current session (expiry is the caller's job — see [EmergencyUnlockPolicy]).
     * @param nowEpochMs current time, used only to evaluate unlock expiry.
     */
    fun decide(
        foregroundPackage: String?,
        selectedBlockedPackages: Set<String>,
        sessionStatus: FocusSessionStatus?,
        activeEmergencyUnlocks: List<EmergencyUnlock>,
        nowEpochMs: Long,
    ): RestrictionDecision {
        if (foregroundPackage == null) return RestrictionDecision.Allow

        // Restriction only ever runs alongside an active, currently-focusing
        // session — paused/break/completed/no-session all mean "allow".
        if (sessionStatus != FocusSessionStatus.FOCUSING) return RestrictionDecision.Allow

        if (foregroundPackage !in selectedBlockedPackages) return RestrictionDecision.Allow

        if (EmergencyUnlockPolicy.hasActiveUnlock(activeEmergencyUnlocks, foregroundPackage, nowEpochMs)) {
            return RestrictionDecision.Allow
        }

        return RestrictionDecision.Block(foregroundPackage)
    }
}

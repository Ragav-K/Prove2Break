package com.prove2break.apprestriction

import com.prove2break.domain.model.EmergencyUnlock

/**
 * Expiry rule for a granted [EmergencyUnlock]: active from the moment it's
 * recorded until [EmergencyUnlock.timestampMillis] + [EmergencyUnlock.durationMillis]
 * has elapsed. Pure function, no clock access, so it's directly unit-testable.
 */
object EmergencyUnlockPolicy {

    fun isActive(unlock: EmergencyUnlock, nowEpochMs: Long): Boolean {
        val expiresAt = unlock.timestampMillis + unlock.durationMillis
        return nowEpochMs in unlock.timestampMillis until expiresAt
    }

    /** True if any unlock in [unlocks] for [packageName] is currently active. */
    fun hasActiveUnlock(unlocks: List<EmergencyUnlock>, packageName: String, nowEpochMs: Long): Boolean {
        return unlocks.any { it.packageName == packageName && isActive(it, nowEpochMs) }
    }
}

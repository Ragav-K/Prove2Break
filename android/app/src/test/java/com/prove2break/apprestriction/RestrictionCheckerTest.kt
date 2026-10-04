package com.prove2break.apprestriction

import com.prove2break.domain.model.EmergencyUnlock
import com.prove2break.domain.model.EmergencyUnlockReason
import com.prove2break.domain.model.FocusSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RestrictionCheckerTest {

    private val now = 1_000_000L

    private fun unlock(packageName: String, timestamp: Long, durationMillis: Long) = EmergencyUnlock(
        id = 1,
        packageName = packageName,
        reason = EmergencyUnlockReason.OTHER,
        timestampMillis = timestamp,
        durationMillis = durationMillis,
        focusSessionId = 42,
    )

    @Test
    fun `allows when foreground package is null`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = null,
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Allow, decision)
    }

    @Test
    fun `allows when there is no active session`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = null,
            activeEmergencyUnlocks = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Allow, decision)
    }

    @Test
    fun `allows when session is paused`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.PAUSED,
            activeEmergencyUnlocks = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Allow, decision)
    }

    @Test
    fun `allows when foreground app is not on the blocked list`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.example.calculator",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Allow, decision)
    }

    @Test
    fun `blocks a selected app during an active focusing session`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = emptyList(),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Block("com.instagram.android"), decision)
    }

    @Test
    fun `allows a blocked app when an active emergency unlock covers it`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = listOf(unlock("com.instagram.android", now - 1_000, 60_000)),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Allow, decision)
    }

    @Test
    fun `blocks again once the emergency unlock has expired`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = listOf(unlock("com.instagram.android", now - 120_000, 60_000)),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Block("com.instagram.android"), decision)
    }

    @Test
    fun `an unlock for a different package does not cover the blocked app`() {
        val decision = RestrictionChecker.decide(
            foregroundPackage = "com.instagram.android",
            selectedBlockedPackages = setOf("com.instagram.android"),
            sessionStatus = FocusSessionStatus.FOCUSING,
            activeEmergencyUnlocks = listOf(unlock("com.twitter.android", now - 1_000, 60_000)),
            nowEpochMs = now,
        )
        assertEquals(RestrictionDecision.Block("com.instagram.android"), decision)
    }
}

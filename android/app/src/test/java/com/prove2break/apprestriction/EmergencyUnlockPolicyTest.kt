package com.prove2break.apprestriction

import com.prove2break.domain.model.EmergencyUnlock
import com.prove2break.domain.model.EmergencyUnlockReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyUnlockPolicyTest {

    private fun unlock(timestamp: Long, durationMillis: Long, packageName: String = "com.instagram.android") =
        EmergencyUnlock(
            id = 1,
            packageName = packageName,
            reason = EmergencyUnlockReason.ACADEMIC,
            timestampMillis = timestamp,
            durationMillis = durationMillis,
            focusSessionId = 7,
        )

    @Test
    fun `is active right at grant time`() {
        val grant = unlock(timestamp = 1000L, durationMillis = 5000L)
        assertTrue(EmergencyUnlockPolicy.isActive(grant, nowEpochMs = 1000L))
    }

    @Test
    fun `is active partway through its duration`() {
        val grant = unlock(timestamp = 1000L, durationMillis = 5000L)
        assertTrue(EmergencyUnlockPolicy.isActive(grant, nowEpochMs = 4000L))
    }

    @Test
    fun `is not active once the duration has fully elapsed`() {
        val grant = unlock(timestamp = 1000L, durationMillis = 5000L)
        assertFalse(EmergencyUnlockPolicy.isActive(grant, nowEpochMs = 6000L))
    }

    @Test
    fun `is not active exactly at the expiry boundary`() {
        val grant = unlock(timestamp = 1000L, durationMillis = 5000L)
        assertFalse(EmergencyUnlockPolicy.isActive(grant, nowEpochMs = 6000L))
    }

    @Test
    fun `is not active before it was granted`() {
        val grant = unlock(timestamp = 1000L, durationMillis = 5000L)
        assertFalse(EmergencyUnlockPolicy.isActive(grant, nowEpochMs = 999L))
    }

    @Test
    fun `hasActiveUnlock finds a matching active grant among several`() {
        val unlocks = listOf(
            unlock(timestamp = 1000L, durationMillis = 5000L, packageName = "com.twitter.android"),
            unlock(timestamp = 2000L, durationMillis = 5000L, packageName = "com.instagram.android"),
        )
        assertTrue(EmergencyUnlockPolicy.hasActiveUnlock(unlocks, "com.instagram.android", nowEpochMs = 3000L))
    }

    @Test
    fun `hasActiveUnlock is false when no grant matches the package`() {
        val unlocks = listOf(unlock(timestamp = 1000L, durationMillis = 5000L, packageName = "com.twitter.android"))
        assertFalse(EmergencyUnlockPolicy.hasActiveUnlock(unlocks, "com.instagram.android", nowEpochMs = 3000L))
    }

    @Test
    fun `hasActiveUnlock is false when the matching grant has expired`() {
        val unlocks = listOf(unlock(timestamp = 1000L, durationMillis = 5000L))
        assertFalse(EmergencyUnlockPolicy.hasActiveUnlock(unlocks, "com.instagram.android", nowEpochMs = 10_000L))
    }
}

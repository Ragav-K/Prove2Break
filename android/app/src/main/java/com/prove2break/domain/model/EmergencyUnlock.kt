package com.prove2break.domain.model

/**
 * A recorded, time-boxed exception granted for one restricted app. Never a
 * silent bypass: every grant is written with its reason, timestamp, app and
 * duration, and expires on its own (see [com.prove2break.apprestriction.EmergencyUnlockPolicy]).
 */
data class EmergencyUnlock(
    val id: Long,
    val packageName: String,
    val reason: EmergencyUnlockReason,
    val timestampMillis: Long,
    val durationMillis: Long,
    val focusSessionId: Long,
)

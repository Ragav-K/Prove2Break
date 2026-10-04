package com.prove2break.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Every emergency-unlock grant, recorded permanently (never silently applied
 * — APP-RESTRICTION.md). [focusSessionId] ties the grant to the session it
 * was requested during; it is not a Room foreign key constraint because focus
 * sessions may be cleared independently and an unlock's audit record should
 * outlive its session row.
 */
@Entity(tableName = "emergency_unlocks")
data class EmergencyUnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val reason: String,
    val timestampMillis: Long,
    val durationMillis: Long,
    val focusSessionId: Long,
)

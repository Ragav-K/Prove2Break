package com.prove2break.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent record of a manually-selected app to restrict during focus
 * sessions. [isSelected] lets the student uncheck an app without losing its
 * label/history — see APP-RESTRICTION.md's Manual App Selection.
 */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val isSelected: Boolean = true,
)

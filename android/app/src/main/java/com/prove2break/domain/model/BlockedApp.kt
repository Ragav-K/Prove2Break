package com.prove2break.domain.model

/**
 * A manually selected app the student wants restricted during focus sessions.
 * See APP-RESTRICTION.md — selection is always manual, never auto-classified.
 */
data class BlockedApp(
    val packageName: String,
    val appLabel: String,
    val isSelected: Boolean,
)

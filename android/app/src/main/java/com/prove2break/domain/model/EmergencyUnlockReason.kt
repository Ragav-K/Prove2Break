package com.prove2break.domain.model

/** Reason categories a student must pick from when requesting emergency access (APP-RESTRICTION.md). */
enum class EmergencyUnlockReason(val displayLabel: String) {
    ACADEMIC("Academic"),
    EMERGENCY("Emergency"),
    COMMUNICATION("Communication"),
    OTHER("Other"),
}

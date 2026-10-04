package com.prove2break.domain.model

/**
 * Focus session lifecycle states. Matches BREAK-SYSTEM.md's state machine names
 * where they overlap so later phases (break system) can extend this enum
 * without a schema rewrite.
 *
 * Phase 5 (Break System) wires up BREAK_AVAILABLE/BREAK_ACTIVE for real and
 * adds BREAK_VERIFICATION/QUIZ_ACTIVE for the unscheduled-break path
 * (BREAK-SYSTEM.md's "Unscheduled path"):
 *
 * FOCUSING -> BREAK_AVAILABLE -> BREAK_ACTIVE -> FOCUSING (scheduled path)
 * FOCUSING -> BREAK_VERIFICATION -> QUIZ_ACTIVE -> BREAK_ACTIVE | FOCUSING (unscheduled path)
 */
enum class FocusSessionStatus {
    FOCUSING,
    PAUSED,
    BREAK_AVAILABLE,
    BREAK_ACTIVE,
    /** Student tapped "Break Now" and is selecting claimed-complete topics. */
    BREAK_VERIFICATION,
    /** Rapid-fire quiz in progress for an unscheduled break request. */
    QUIZ_ACTIVE,
    COMPLETED,
}

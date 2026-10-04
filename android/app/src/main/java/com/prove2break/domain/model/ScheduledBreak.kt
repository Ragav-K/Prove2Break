package com.prove2break.domain.model

/** Lifecycle of a single scheduled or unscheduled break occurrence. */
enum class ScheduledBreakStatus {
    /** Not yet taken. Covers "not due yet" and "due, deferred/re-offerable" — see BREAK-SYSTEM.md's rule that
     * "continue studying" never consumes a break, it just leaves it PENDING for later. */
    PENDING,
    /** The student tapped "Take Break" — currently running. */
    ACTIVE,
    /** Break ended (timer completed or ended early). Terminal for this occurrence. */
    TAKEN,
}

/**
 * A single break occurrence for a focus session — either generated up front
 * from [com.prove2break.breaks.BreakRecommendation] ("scheduled") or created
 * on the fly by the unscheduled "Break Now" -> verification flow.
 */
data class ScheduledBreak(
    val id: Long,
    val sessionId: Long,
    val offsetMillis: Long,
    val plannedLengthMillis: Long,
    val status: ScheduledBreakStatus,
    val activeStartedAtEpochMs: Long?,
    val extendedMillis: Long,
    val isUnscheduled: Boolean,
)

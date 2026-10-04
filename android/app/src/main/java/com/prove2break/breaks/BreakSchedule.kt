package com.prove2break.breaks

import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus

/**
 * Pure derivation of "is a break available right now" from a focus session's
 * already-computed elapsed time (see [com.prove2break.focus.FocusTimer]) and
 * its list of [ScheduledBreak]s. Deliberately takes elapsed millis rather than
 * recomputing timing itself: [com.prove2break.focus.FocusTimer.computeSnapshot]
 * already folds pause time out of elapsed time, so a break offset compared
 * against that same elapsed value is automatically pause-aware — a session
 * paused right up to a break's offset does not fire that break early, exactly
 * like BREAK-SYSTEM.md requires for session timing generally.
 *
 * BREAK-SYSTEM.md rule: "continue studying" never auto-starts a break and
 * never consumes it — a break that is due but not yet ACTIVE/TAKEN stays
 * PENDING and keeps being the "current available break" every time this is
 * recomputed, until the student explicitly takes it.
 */
object BreakSchedule {

    /**
     * The earliest PENDING break whose offset has been reached (`elapsedMillis
     * >= offsetMillis`), or null if none is due yet. Only one break is ever
     * "available" at a time by design (earliest-due wins) — later breaks stay
     * PENDING until earlier ones are resolved.
     */
    fun currentAvailableBreak(elapsedMillis: Long, breaks: List<ScheduledBreak>): ScheduledBreak? {
        return breaks
            .asSequence()
            .filter { it.status == ScheduledBreakStatus.PENDING }
            .filter { elapsedMillis >= it.offsetMillis }
            .minByOrNull { it.offsetMillis }
    }
}

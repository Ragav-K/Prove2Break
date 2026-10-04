package com.prove2break.focus

import com.prove2break.domain.model.FocusSession

/** A focus session plus its current, freshly-derived timer reading. */
data class FocusSessionSnapshot(
    val session: FocusSession,
    val timer: TimerSnapshot,
)

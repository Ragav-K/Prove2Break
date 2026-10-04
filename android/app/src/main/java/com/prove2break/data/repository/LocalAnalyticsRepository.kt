package com.prove2break.data.repository

import com.prove2break.domain.model.LocalStudyStats

/** Aggregates Android-local (Room) focus-session/break history into [LocalStudyStats]. */
interface LocalAnalyticsRepository {
    suspend fun getLocalStudyStats(): LocalStudyStats
}

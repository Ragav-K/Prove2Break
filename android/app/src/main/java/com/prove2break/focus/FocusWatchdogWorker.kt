package com.prove2break.focus

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prove2break.data.repository.FocusSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Watchdog described in ANDROID-ARCHITECTURE.md's WorkManager Usage: if Room
 * shows an active [com.prove2break.domain.model.FocusSessionStatus.FOCUSING]
 * / `PAUSED` session but [FocusService] isn't running, restart it. Runs
 * periodically (WorkManager's minimum periodic interval is 15 minutes) — a
 * documented tradeoff, not a precise real-time guarantee; the foreground
 * service's own `START_STICKY` return value is the first line of defense
 * against the OS killing it mid-session.
 */
@HiltWorker
class FocusWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: FocusSessionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activeSession = repository.getActiveSession()
        if (activeSession != null && !FocusService.isRunning) {
            FocusService.start(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "focus_watchdog"
    }
}

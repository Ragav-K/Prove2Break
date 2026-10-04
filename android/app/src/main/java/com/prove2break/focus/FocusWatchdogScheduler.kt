package com.prove2break.focus

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules/cancels the [FocusWatchdogWorker] alongside a focus session's
 * lifecycle (started when a session starts, cancelled once it ends).
 */
object FocusWatchdogScheduler {
    private val MIN_INTERVAL = 15L to TimeUnit.MINUTES

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<FocusWatchdogWorker>(MIN_INTERVAL.first, MIN_INTERVAL.second)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FocusWatchdogWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FocusWatchdogWorker.WORK_NAME)
    }
}

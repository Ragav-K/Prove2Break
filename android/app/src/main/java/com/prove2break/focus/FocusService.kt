package com.prove2break.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.prove2break.apprestriction.AppRestrictionMonitor
import com.prove2break.breaks.BreakManager
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.notifications.FocusNotifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps a focus session alive in the background.
 *
 * Per ANDROID-ARCHITECTURE.md's Foreground Service Design: on creation it
 * does not assume a fresh start — [FocusSessionManager.recoverActiveSession]
 * is consulted first so a service restarted by the OS (or by the
 * [FocusWatchdogWorker]) picks the session back up mid-flight rather than
 * resetting anything. It stops itself only on session completion or an
 * explicit end-session action; [START_STICKY] covers the OS killing the
 * service process itself while a session is still active.
 */
@AndroidEntryPoint
class FocusService : Service() {

    @Inject
    lateinit var manager: FocusSessionManager

    @Inject
    lateinit var breakManager: BreakManager

    @Inject
    lateinit var restrictionMonitor: AppRestrictionMonitor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null
    private var restrictionJob: Job? = null
    private var breakNotificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        FocusNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Post a placeholder notification immediately (required within a few
        // seconds of startForegroundService); recovery/observation below
        // replaces it with the real remaining time on the next tick.
        startForeground(
            FocusNotifications.ONGOING_NOTIFICATION_ID,
            FocusNotifications.buildOngoingNotification(this, 0L, isPaused = false),
        )
        observeSession()
        return START_STICKY
    }

    @Volatile
    private var currentSessionStatus: FocusSessionStatus? = null

    private fun observeSession() {
        if (observeJob != null) return
        observeJob = serviceScope.launch {
            // Recovery path: covers this service instance being (re)started
            // after the process/service was killed while a session was active.
            manager.recoverActiveSession()

            manager.observeCurrentSession().onEach { snapshot ->
                if (snapshot == null) {
                    currentSessionStatus = null
                    stopSelf()
                    return@onEach
                }

                manager.completeIfDue(snapshot)
                currentSessionStatus = snapshot.session.status
                ensureRestrictionMonitoring(snapshot.session.id)
                ensureBreakNotificationMonitoring(snapshot.session.id)

                if (snapshot.timer.isComplete && snapshot.session.status != FocusSessionStatus.BREAK_ACTIVE) {
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager?.notify(
                        FocusNotifications.COMPLETION_NOTIFICATION_ID,
                        FocusNotifications.buildCompletionNotification(this@FocusService),
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateOngoingNotification(
                        FocusNotifications.BreakAwareState.Focusing(
                            remainingMillis = snapshot.timer.remainingMillis,
                            isPaused = snapshot.session.status == FocusSessionStatus.PAUSED,
                        ),
                    )
                }
            }.launchIn(this)
        }
    }

    private fun updateOngoingNotification(state: FocusNotifications.BreakAwareState) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(
            FocusNotifications.ONGOING_NOTIFICATION_ID,
            FocusNotifications.buildOngoingNotification(this@FocusService, state),
        )
    }

    /**
     * Overrides the plain focus-timer notification text with break-state text
     * (BREAK-SYSTEM.md: "Break available" / "On break — Xm remaining" /
     * "Verifying your learning") whenever the session is in one of those
     * states. Runs alongside [observeSession]'s own notification updates —
     * whichever emits last for a given tick wins, which in practice is fine
     * since both observe the same underlying session status.
     */
    private fun ensureBreakNotificationMonitoring(sessionId: Long) {
        if (breakNotificationJob != null) return
        breakNotificationJob = serviceScope.launch {
            breakManager.observeAvailableBreak(sessionId, manager.observeCurrentSession()).onEach { available ->
                if (currentSessionStatus == FocusSessionStatus.FOCUSING && available != null) {
                    updateOngoingNotification(FocusNotifications.BreakAwareState.BreakAvailable)
                }
            }.launchIn(this)

            breakManager.observeActiveBreakTimer(sessionId).onEach { activeTimer ->
                if (currentSessionStatus == FocusSessionStatus.BREAK_ACTIVE && activeTimer != null) {
                    updateOngoingNotification(FocusNotifications.BreakAwareState.OnBreak(activeTimer.second.remainingMillis))
                }
            }.launchIn(this)
        }
    }

    /**
     * Starts the app-restriction polling loop (see [AppRestrictionMonitor])
     * once, for whichever session is currently active. It reads
     * [currentSessionStatus] on every poll rather than being restarted per
     * status change, so it naturally no-ops while PAUSED/BREAK and resumes
     * checking once back to FOCUSING.
     */
    private fun ensureRestrictionMonitoring(sessionId: Long) {
        if (restrictionJob != null) return
        restrictionJob = serviceScope.launch {
            restrictionMonitor.run(
                sessionId = sessionId,
                sessionStatusProvider = { currentSessionStatus },
            )
        }
    }

    override fun onDestroy() {
        isRunning = false
        observeJob?.cancel()
        restrictionJob?.cancel()
        breakNotificationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** Best-effort in-process flag consulted by [FocusWatchdogWorker]. */
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FocusService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusService::class.java))
        }
    }
}

private typealias NotificationManager = android.app.NotificationManager

package com.prove2break.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.prove2break.app.MainActivity

/**
 * Notification builders + channel registration for the focus session
 * foreground service (see ANDROID-ARCHITECTURE.md's Foreground Service
 * Design and NOTIFICATION-SYSTEM.md).
 */
object FocusNotifications {
    const val CHANNEL_ID = "focus_session"
    const val ONGOING_NOTIFICATION_ID = 1001
    const val COMPLETION_NOTIFICATION_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows remaining time while a focus session is active"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildOngoingNotification(context: Context, remainingMillis: Long, isPaused: Boolean): android.app.Notification {
        return buildOngoingNotification(context, BreakAwareState.Focusing(remainingMillis, isPaused))
    }

    /**
     * State the ongoing notification's title/text should reflect
     * (BREAK-SYSTEM.md): "Break available" vs "Focusing — Xm remaining" vs
     * "On break — Xm remaining", extending the original focus-only text.
     */
    sealed class BreakAwareState {
        data class Focusing(val remainingMillis: Long, val isPaused: Boolean) : BreakAwareState()
        data object BreakAvailable : BreakAwareState()
        data class OnBreak(val remainingMillis: Long) : BreakAwareState()
        data object Verifying : BreakAwareState()
    }

    fun buildOngoingNotification(context: Context, state: BreakAwareState): android.app.Notification {
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val (title, text) = when (state) {
            is BreakAwareState.Focusing -> {
                val title = if (state.isPaused) "Focus session paused" else "Focus session in progress"
                title to "${formatRemaining(state.remainingMillis)} remaining"
            }
            is BreakAwareState.BreakAvailable -> "Break available" to "Tap to take your break, or keep studying."
            is BreakAwareState.OnBreak -> "On break" to "${formatRemaining(state.remainingMillis)} remaining"
            is BreakAwareState.Verifying -> "Verifying your learning" to "Answer a few quick questions to unlock your break."
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildCompletionNotification(context: Context): android.app.Notification {
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Focus session complete")
            .setContentText("Nice work — your planned session is done.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun formatRemaining(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

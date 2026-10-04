package com.prove2break.apprestriction

import android.content.Context
import android.content.Intent
import com.prove2break.data.repository.BlockedAppRepository
import com.prove2break.data.repository.EmergencyUnlockRepository
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.ui.apprestriction.BlockedAppActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polling loop run alongside [com.prove2break.focus.FocusService]: while a
 * session is FOCUSING, periodically asks [ForegroundAppDetector] which app is
 * frontmost and, via [RestrictionChecker], decides whether to launch the
 * blocked-app interstitial ([com.prove2break.ui.apprestriction.BlockedAppActivity]).
 *
 * Mechanism: `UsageStatsManager` polling (see APP-RESTRICTION.md's Possible
 * Mechanisms) at [pollIntervalMillis] — not an AccessibilityService. This is
 * an honest tradeoff: simpler, lower user-trust bar, no extra Play Store
 * review scrutiny, but with real detection latency (a blocked app can be
 * visible for up to ~[pollIntervalMillis] before the interstitial appears)
 * and it stops working if the student hasn't granted Usage Access or has
 * battery-optimized the app into suspension.
 */
@Singleton
class AppRestrictionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedAppRepository: BlockedAppRepository,
    private val emergencyUnlockRepository: EmergencyUnlockRepository,
) {

    /** Runs forever (until the calling coroutine is cancelled), checking every [pollIntervalMillis]. */
    suspend fun run(
        sessionId: Long,
        sessionStatusProvider: suspend () -> FocusSessionStatus?,
        pollIntervalMillis: Long = 2_000L,
    ) {
        while (true) {
            checkOnce(sessionId, sessionStatusProvider())
            delay(pollIntervalMillis)
        }
    }

    suspend fun checkOnce(sessionId: Long, sessionStatus: FocusSessionStatus?) {
        if (sessionStatus != FocusSessionStatus.FOCUSING) return
        if (!UsageAccessHelper.hasUsageAccess(context)) return

        val blockedPackages = blockedAppRepository.getSelectedPackageNames()
        if (blockedPackages.isEmpty()) return

        val foregroundPackage = ForegroundAppDetector.getForegroundPackage(context)
        val activeUnlocks = emergencyUnlockRepository.observeForSession(sessionId).first()

        val decision = RestrictionChecker.decide(
            foregroundPackage = foregroundPackage,
            selectedBlockedPackages = blockedPackages,
            sessionStatus = sessionStatus,
            activeEmergencyUnlocks = activeUnlocks,
            nowEpochMs = System.currentTimeMillis(),
        )

        if (decision is RestrictionDecision.Block) {
            launchBlockedAppScreen(decision.packageName, sessionId)
        }
    }

    private fun launchBlockedAppScreen(packageName: String, sessionId: Long) {
        val intent = Intent(context, BlockedAppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        context.startActivity(intent)
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_SESSION_ID = "session_id"
    }
}

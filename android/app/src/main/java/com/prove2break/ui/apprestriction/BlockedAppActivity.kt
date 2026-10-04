package com.prove2break.ui.apprestriction

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prove2break.app.MainActivity
import com.prove2break.apprestriction.AppRestrictionMonitor
import com.prove2break.ui.common.theme.Prove2BreakTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The interstitial's host Activity — launched from outside the app's own nav
 * graph (by [AppRestrictionMonitor], from a background service context), so
 * it needs to be its own task/Activity rather than a NavHost destination
 * reachable only from within Prove2Break's foreground UI.
 */
@AndroidEntryPoint
class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra(AppRestrictionMonitor.EXTRA_BLOCKED_PACKAGE) ?: ""
        val sessionId = intent.getLongExtra(AppRestrictionMonitor.EXTRA_SESSION_ID, 0L)
        val blockedLabel = resolveLabel(blockedPackage)

        setContent {
            Prove2BreakTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "blocked") {
                    composable("blocked") {
                        BlockedAppScreen(
                            blockedAppLabel = blockedLabel,
                            onReturnToProve2Break = { returnToProve2Break() },
                            onRequestEmergencyAccess = { navController.navigate("emergency_unlock") },
                        )
                    }
                    composable("emergency_unlock") {
                        EmergencyUnlockScreen(
                            packageName = blockedPackage,
                            sessionId = sessionId,
                            onGranted = { finish() },
                            onCancel = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    private fun returnToProve2Break() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun resolveLabel(packageName: String): String {
        if (packageName.isBlank()) return "This app"
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }
}

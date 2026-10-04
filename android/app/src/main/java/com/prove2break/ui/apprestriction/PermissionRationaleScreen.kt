package com.prove2break.ui.apprestriction

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.apprestriction.UsageAccessHelper

/**
 * Shown once, before app-selection: the honest-limitations disclosure
 * (APP-RESTRICTION.md) followed by an explainer for each sensitive permission
 * App Restriction needs, each stating why it's needed, what happens if
 * declined, and what still works either way — before the settings screen or
 * system dialog is ever triggered (PERMISSIONS.md's hard requirement).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRationaleScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: PermissionRationaleViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var disclosureAcknowledged by rememberSaveable { mutableStateOf(viewModel.hasSeenDisclosure) }
    var usageAccessGranted by remember { mutableStateOf(viewModel.hasUsageAccess()) }
    var notificationsHandled by rememberSaveable {
        mutableStateOf(!viewModel.requiresNotificationRuntimePermission)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsHandled = true }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Before You Restrict Apps") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "What Prove2Break can and can't do", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Prove2Break makes it meaningfully harder to open distracting apps " +
                                "during focus — not impossible. It's a strong nudge and friction layer, " +
                                "not a lock. Android doesn't give a regular app a guaranteed, unbypassable " +
                                "way to block another app.",
                        )
                        Text(
                            text = "A determined user can always get around it (Safe Mode, uninstalling " +
                                "Prove2Break, a second user profile). Foreground-app detection also runs on " +
                                "a short delay, so a restricted app can flash on screen briefly before the " +
                                "block screen appears. Some phone manufacturers' battery optimization can " +
                                "pause detection unless you exempt Prove2Break in Settings.",
                        )
                        if (!disclosureAcknowledged) {
                            Button(onClick = {
                                viewModel.markDisclosureSeen()
                                disclosureAcknowledged = true
                            }) {
                                Text("I understand")
                            }
                        }
                    }
                }
            }
            item {
                PermissionCard(
                    title = "Usage Access",
                    why = "Needed to detect which app is currently open, so Prove2Break can show the " +
                        "block screen when a restricted app is opened during focus.",
                    ifDenied = "App restriction won't work — restricted apps stay freely accessible. " +
                        "The focus timer, breaks, quizzes and analytics all keep working normally.",
                    actionLabel = if (usageAccessGranted) "Granted" else "Open Settings",
                    actionEnabled = !usageAccessGranted,
                    onAction = {
                        context.startActivity(UsageAccessHelper.buildUsageAccessSettingsIntent())
                        usageAccessGranted = UsageAccessHelper.hasUsageAccess(context)
                    },
                )
            }
            if (viewModel.requiresNotificationRuntimePermission) {
                item {
                    PermissionCard(
                        title = "Notifications",
                        why = "Needed to show the ongoing focus-session notification and alerts for " +
                            "breaks, quiz results and session status.",
                        ifDenied = "No proactive alerts are shown. The timer, restriction and quiz still " +
                            "function fully — you can enable notifications later in Settings.",
                        actionLabel = if (notificationsHandled) "Granted" else "Allow Notifications",
                        actionEnabled = !notificationsHandled,
                        onAction = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                    )
                }
            }
            item {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue to App Selection")
                }
            }
            item {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    why: String,
    ifDenied: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Why: $why")
            Text(text = "If you decline: $ifDenied")
            Button(onClick = onAction, enabled = actionEnabled) {
                Text(actionLabel)
            }
        }
    }
}

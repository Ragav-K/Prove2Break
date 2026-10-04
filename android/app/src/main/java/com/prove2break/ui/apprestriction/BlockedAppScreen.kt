package com.prove2break.ui.apprestriction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-screen interstitial shown when a restricted app is detected in the
 * foreground during an active focus session (APP-RESTRICTION.md). Deliberately
 * plain-spoken about the mechanism's limits rather than implying an
 * unbypassable lock.
 */
@Composable
fun BlockedAppScreen(
    blockedAppLabel: String,
    onReturnToProve2Break: () -> Unit,
    onRequestEmergencyAccess: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "This app is restricted during your focus session",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(text = "$blockedAppLabel is on your restricted list while you're focusing.")
            Text(text = "Need it urgently? You can request temporary, recorded access instead of fighting this screen.")

            Button(onClick = onReturnToProve2Break, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Prove2Break")
            }
            Button(onClick = onRequestEmergencyAccess, modifier = Modifier.fillMaxWidth()) {
                Text("Request Emergency Access")
            }
        }
    }
}

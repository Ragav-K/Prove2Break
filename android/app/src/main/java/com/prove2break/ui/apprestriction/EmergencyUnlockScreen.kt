package com.prove2break.ui.apprestriction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.EmergencyUnlockReason

/**
 * Reason-picker for a temporary, recorded exception to restriction
 * (APP-RESTRICTION.md's Emergency Unlock). Never grants silently: the student
 * must pick a reason category and a bounded duration before access resumes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyUnlockScreen(
    packageName: String,
    sessionId: Long,
    onGranted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: EmergencyUnlockViewModel = hiltViewModel(),
) {
    var selectedReason by remember { mutableStateOf<EmergencyUnlockReason?>(null) }
    var selectedDurationMillis by remember { mutableStateOf(EmergencyUnlockViewModel.DEFAULT_DURATION_OPTIONS.first().first) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Request Emergency Access") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "This request, its reason and duration are recorded — access is never granted silently.")

            Text(text = "Why do you need access?")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EmergencyUnlockReason.entries.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason.displayLabel) },
                    )
                }
            }

            Text(text = "For how long?")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EmergencyUnlockViewModel.DEFAULT_DURATION_OPTIONS.forEach { (millis, label) ->
                    FilterChip(
                        selected = selectedDurationMillis == millis,
                        onClick = { selectedDurationMillis = millis },
                        label = { Text(label) },
                    )
                }
            }

            Button(
                onClick = {
                    val reason = selectedReason ?: return@Button
                    viewModel.requestUnlock(
                        packageName = packageName,
                        sessionId = sessionId,
                        reason = reason,
                        durationMillis = selectedDurationMillis,
                        onGranted = onGranted,
                    )
                },
                enabled = selectedReason != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant Temporary Access")
            }

            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

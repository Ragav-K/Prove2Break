package com.prove2break.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.FocusSessionStatus

/**
 * Live focus session screen. Every displayed time is recomputed from the
 * manager's snapshot flow (timestamp-derived — never a locally-decrementing
 * counter, per FOCUS-ENGINE.md), so leaving/returning to this screen, or the
 * process dying and restarting, always shows the correct value.
 *
 * Also hosts the scheduled-break-available banner (BREAK-SYSTEM.md): "Take
 * Break" moves to [com.prove2break.ui.breaks.BreakActiveScreen]; "Continue
 * Studying" leaves the break PENDING/re-offerable rather than consuming it.
 * "Break Now" is the unscheduled-break entry point, independent of whether a
 * scheduled break is currently available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    sessionId: Long,
    onSessionEnded: () -> Unit,
    onTakeBreak: (breakId: Long) -> Unit,
    onBreakNow: (subjectId: String) -> Unit,
    viewModel: FocusModeViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.sessionState.collectAsState()
    val availableBreak by viewModel.availableBreak.collectAsState()

    LaunchedEffect(snapshot) {
        if (snapshot == null) {
            onSessionEnded()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Focus Mode") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val current = snapshot
            if (current == null) {
                Text(text = "Session complete.")
            } else {
                val isPaused = current.session.status == FocusSessionStatus.PAUSED
                Text(
                    text = formatDuration(current.timer.remainingMillis),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(text = if (isPaused) "Paused" else "Focusing")

                val dueBreak = availableBreak
                if (dueBreak != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "A break is available.", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(onClick = {
                                    viewModel.takeBreak(sessionId, dueBreak.id)
                                    onTakeBreak(dueBreak.id)
                                }) {
                                    Text("Take Break")
                                }
                                OutlinedButton(onClick = { viewModel.continueStudying(sessionId) }) {
                                    Text("Continue Studying")
                                }
                            }
                        }
                    }
                }

                if (isPaused) {
                    Button(onClick = { viewModel.resume(sessionId) }) {
                        Text("Resume")
                    }
                } else {
                    Button(onClick = { viewModel.pause(sessionId) }) {
                        Text("Pause")
                    }
                }

                OutlinedButton(onClick = { onBreakNow(current.session.subjectId) }) {
                    Text("Break Now")
                }

                Button(onClick = { viewModel.endSession(sessionId) }) {
                    Text("End Session")
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

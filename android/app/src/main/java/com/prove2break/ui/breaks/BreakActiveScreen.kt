package com.prove2break.ui.breaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Break-active screen: live countdown derived from timestamps (never a
 * decrementing counter, mirroring [com.prove2break.focus.FocusTimer]'s
 * discipline), "End Break Early", and a bounded "Extend Break" that disables
 * itself once the session's extension budget is exhausted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakActiveScreen(
    onBreakEnded: () -> Unit,
    viewModel: BreakActiveViewModel = hiltViewModel(),
) {
    val active by viewModel.activeBreak.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val extensionDenied by viewModel.extensionDenied.collectAsState()

    LaunchedEffect(active) {
        if (active == null) {
            onBreakEnded()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("On Break") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val current = active
            if (current == null) {
                Text(text = "Break complete — back to focusing.")
            } else {
                val (_, timer) = current
                Text(
                    text = formatRemaining(timer.remainingMillis),
                    style = MaterialTheme.typography.displayMedium,
                )
                Text(text = "remaining on your break")

                val canExtend = (budget?.remainingMillis ?: 0L) > 0
                Button(onClick = { viewModel.extendBreak() }, enabled = canExtend) {
                    Text(if (canExtend) "Extend Break (+5 min)" else "Extend Break (limit reached)")
                }
                if (extensionDenied) {
                    Text(text = "You've used all the extra break time available for this session.")
                }

                OutlinedButton(onClick = { viewModel.endBreakEarly() }) {
                    Text("End Break Early")
                }
            }
        }
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

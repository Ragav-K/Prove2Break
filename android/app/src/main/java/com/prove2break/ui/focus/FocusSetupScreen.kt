package com.prove2break.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.usecase.StartFocusSessionUseCase

private data class DurationOption(val label: String, val minutes: Int)

private val PRESET_DURATIONS = listOf(
    DurationOption("1 hour", 60),
    DurationOption("2 hours", 120),
    DurationOption("3 hours", 180),
    DurationOption("4 hours", 240),
)

/**
 * Duration picker for starting a focus session (FOCUS-ENGINE.md's Supported
 * Durations: 1h/2h/3h/4h presets plus a bounded custom entry).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSetupScreen(
    onSessionStarted: (Long) -> Unit,
    onBack: () -> Unit,
    onCreateSubject: () -> Unit = onBack,
    viewModel: FocusSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMinutes by remember { mutableStateOf(60) }
    var customMinutesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCustomSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Focus Setup") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!uiState.isLoadingSubjects && uiState.subjects.isEmpty()) {
                item {
                    Text(text = "You need a subject before starting a focus session.")
                }
                item {
                    Button(onClick = onCreateSubject, modifier = Modifier.fillMaxWidth()) {
                        Text("Create a subject")
                    }
                }
                item {
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
                }
                return@LazyColumn
            }
            item {
                Text(text = "Subject")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.subjects.forEach { subject ->
                        FilterChip(
                            selected = uiState.selectedSubjectId == subject.id,
                            onClick = { viewModel.selectSubject(subject.id) },
                            label = { Text(subject.name) },
                        )
                    }
                }
            }
            item {
                Text(text = "How long do you want to focus?")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_DURATIONS.forEach { option ->
                        FilterChip(
                            selected = !isCustomSelected && selectedMinutes == option.minutes,
                            onClick = {
                                isCustomSelected = false
                                selectedMinutes = option.minutes
                                errorMessage = null
                            },
                            label = { Text(option.label) },
                        )
                    }
                    FilterChip(
                        selected = isCustomSelected,
                        onClick = {
                            isCustomSelected = true
                            errorMessage = null
                        },
                        label = { Text("Custom") },
                    )
                }
            }
            if (isCustomSelected) {
                item {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = {
                            customMinutesText = it.filter { c -> c.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Custom duration (minutes)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(text = "Between 15 minutes and 8 hours (480 minutes).")
                }
            }
            errorMessage?.let { message ->
                item { Text(text = message) }
            }
            item {
                Button(
                    onClick = {
                        val minutes = if (isCustomSelected) customMinutesText.toIntOrNull() else selectedMinutes
                        if (minutes == null) {
                            errorMessage = "Enter a valid number of minutes"
                            return@Button
                        }
                        val durationMillis = minutes * 60_000L
                        if (durationMillis < StartFocusSessionUseCase.MIN_DURATION_MILLIS ||
                            durationMillis > StartFocusSessionUseCase.MAX_DURATION_MILLIS
                        ) {
                            errorMessage = "Duration must be between 15 minutes and 8 hours"
                            return@Button
                        }
                        viewModel.startSession(
                            plannedDurationMillis = durationMillis,
                            onStarted = onSessionStarted,
                            onError = { errorMessage = it },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Start Focus Session")
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

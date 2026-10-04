package com.prove2break.ui.breaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * "Break Now" -> BREAK_VERIFICATION step: student selects which topics they
 * claim to have completed since the last verification, pulled from the real
 * [com.prove2break.data.repository.TopicRepository] (Phase 4), not a mock
 * list. "Verify My Learning" advances to QUIZ_ACTIVE.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicChecklistScreen(
    onVerify: (List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: UnscheduledBreakViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Which topics have you covered?") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text(text = "Pick the topics you've studied since your last check — we'll quiz you on just these.")

            if (state.isLoadingTopics) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else if (state.topics.isEmpty()) {
                Text(text = "No topics found for this subject yet.", modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                    items(state.topics, key = { it.id }) { topic ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = topic.id in state.selectedTopicIds,
                                onCheckedChange = { viewModel.toggleTopic(topic.id) },
                            )
                            Text(text = topic.name)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.startVerification(onStarted = onVerify) },
                    enabled = state.selectedTopicIds.isNotEmpty(),
                ) {
                    Text("Verify My Learning")
                }
            }
        }
    }
}

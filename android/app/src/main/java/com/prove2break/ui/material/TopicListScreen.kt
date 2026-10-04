package com.prove2break.ui.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.Topic

/**
 * Topics are grouped by unit_id when present, but always editable — the
 * AI-generated topic structure is never forced on the user (backend
 * TopicUpdate docstring / docs/PROCESS.md acceptance criteria #3): every
 * topic's name is tap-to-rename and its completion is a plain checkbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    onBack: () -> Unit,
    viewModel: TopicListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var renamingTopic by remember { mutableStateOf<Topic?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Topics") }) },
    ) { innerPadding ->
        val grouped = uiState.topics.groupBy { it.unitId }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (unitId, topics) ->
                item {
                    Text(text = unitId?.let { "Unit $it" } ?: "Ungrouped")
                }
                items(topics, key = { it.id }) { topic ->
                    TopicRow(
                        topic = topic,
                        onToggle = { viewModel.toggleCompletion(topic) },
                        onRenameClick = { renamingTopic = topic },
                    )
                }
            }
            uiState.errorMessage?.let { message ->
                item { Text(text = message) }
            }
            item {
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
    }

    renamingTopic?.let { topic ->
        RenameTopicDialog(
            initialName = topic.name,
            onConfirm = { newName ->
                viewModel.renameTopic(topic.id, newName)
                renamingTopic = null
            },
            onDismiss = { renamingTopic = null },
        )
    }
}

@Composable
private fun TopicRow(topic: Topic, onToggle: () -> Unit, onRenameClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = topic.isCompleted, onCheckedChange = { onToggle() })
        Text(
            text = topic.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        TextButton(onClick = onRenameClick) { Text("Rename") }
    }
}

@Composable
private fun RenameTopicDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Rename topic") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        },
    )
}

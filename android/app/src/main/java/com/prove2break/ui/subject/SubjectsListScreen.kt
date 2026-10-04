package com.prove2break.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.prove2break.domain.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsListScreen(
    onSubjectClick: (Subject) -> Unit,
    onCreateSubject: () -> Unit,
    onBack: () -> Unit,
    viewModel: SubjectsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Subjects") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateSubject) {
                Icon(Icons.Default.Add, contentDescription = "Create subject")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.errorMessage != null -> Text(
                    text = uiState.errorMessage ?: "",
                    modifier = Modifier.padding(24.dp),
                )
                uiState.subjects.isEmpty() -> Text(
                    text = "No subjects yet. Tap + to create one.",
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.subjects, key = { it.id }) { subject ->
                        SubjectCard(subject = subject, onClick = { onSubjectClick(subject) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = subject.name)
            subject.examName?.let { Text(text = "Exam: $it") }
            subject.examDate?.let { Text(text = "Date: $it") }
            val total = subject.topicsTotal
            val completed = subject.topicsCompleted
            if (total > 0) {
                val progress = completed.toFloat() / total.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Text(text = "$completed / $total topics completed")
            } else {
                Text(text = "No topics yet")
            }
        }
    }
}

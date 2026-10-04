package com.prove2break.ui.material

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.StudyMaterial
import com.prove2break.domain.usecase.UploadMaterialState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    onEditSubject: (String) -> Unit,
    onViewTopics: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SubjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.uploadMaterial(uri)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(uiState.subject?.name ?: "Subject") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                uiState.subject?.let { subject ->
                    Column {
                        subject.examName?.let { Text(text = "Exam: $it") }
                        subject.examDate?.let { Text(text = "Date: $it") }
                        subject.description?.let { Text(text = it) }
                    }
                }
            }
            item {
                Button(onClick = { onEditSubject(viewModel.subjectId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit Subject")
                }
            }
            item {
                Button(onClick = { onViewTopics(viewModel.subjectId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View Topics")
                }
            }
            item {
                Button(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Upload Material (PDF)")
                }
            }
            uiState.uploadState?.let { state ->
                item { UploadStatusRow(state) }
            }
            item { Text(text = "Materials") }
            items(uiState.materials, key = { it.id }) { material ->
                MaterialCard(material)
            }
            uiState.errorMessage?.let { message ->
                item { Text(text = message) }
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
private fun UploadStatusRow(state: UploadMaterialState) {
    val text = when (state) {
        is UploadMaterialState.Uploading -> "Uploading..."
        is UploadMaterialState.Polling -> "Processing (${state.material.status})..."
        is UploadMaterialState.Ready -> "Ready: ${state.material.filename}"
        is UploadMaterialState.Failed -> "Failed: ${state.message}"
    }
    Text(text = text)
}

@Composable
private fun MaterialCard(material: StudyMaterial) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = material.filename)
            Text(text = "Status: ${material.status}")
            material.failureReason?.let { Text(text = it) }
        }
    }
}

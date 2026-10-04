package com.prove2break.ui.apprestriction

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.InstalledAppInfo

/**
 * Manual app-selection screen (APP-RESTRICTION.md): every installed,
 * launchable app is browsable/searchable-by-scroll with a checkbox; Prove2Break
 * never guesses which apps are "distracting."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    onDone: () -> Unit,
    viewModel: AppSelectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose Apps to Restrict") }) },
    ) { innerPadding ->
        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(32.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        isSelected = app.packageName in state.selectedPackageNames,
                        onToggle = { viewModel.toggle(app) },
                    )
                }
                item {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text("Done (${state.selectedPackageNames.size} selected)")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppInfo, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val bitmap = app.icon?.toBitmap()?.asImageBitmap()
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Text(text = app.label, modifier = Modifier.weight(1f))
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
    }
}

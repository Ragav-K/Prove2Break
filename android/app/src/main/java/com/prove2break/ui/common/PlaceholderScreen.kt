package com.prove2break.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A minimal, correctly-wired stand-in for a not-yet-implemented feature screen.
 * Real screens replace this with their own Composable + ViewModel per
 * ANDROID-ARCHITECTURE.md's MVVM pattern.
 *
 * [actions] lets the NavHost wire real navigation between placeholders so the
 * app is actually click-through-able before each feature screen is built.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    actions: List<PlaceholderAction> = emptyList(),
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title)
            actions.forEach { action ->
                Button(onClick = action.onClick) {
                    Text(text = action.label)
                }
            }
        }
    }
}

data class PlaceholderAction(val label: String, val onClick: () -> Unit)

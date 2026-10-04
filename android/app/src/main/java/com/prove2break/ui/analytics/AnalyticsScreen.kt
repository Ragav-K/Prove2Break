package com.prove2break.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prove2break.domain.model.AnalyticsOverview
import com.prove2break.domain.model.LocalStudyStats
import com.prove2break.domain.model.TopicPerformanceSummary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.errorMessage != null -> Text(
                    text = uiState.errorMessage ?: "",
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { HeadlineStatsCard(uiState.localStats) }
                    item { QuizPerformanceCard(uiState.quizOverview) }
                    item { BreakBehaviorCard(uiState.localStats) }
                    if (uiState.weakTopics.isNotEmpty()) {
                        item { WeakTopicsCard(uiState.weakTopics) }
                    }
                    if (uiState.topicPerformance.isNotEmpty()) {
                        item { Text("All topics", style = MaterialTheme.typography.titleMedium) }
                        items(uiState.topicPerformance, key = { it.topicId }) { topic ->
                            TopicPerformanceRow(topic)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadlineStatsCard(stats: LocalStudyStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Study time", style = MaterialTheme.typography.titleMedium)
            Text(text = formatDuration(stats.totalStudyTimeMillis))
            Text(text = "${stats.completedSessionCount} sessions completed, ${stats.endedEarlyCount} ended early")
            Text(text = "Current streak: ${stats.currentStreakDays} day${if (stats.currentStreakDays == 1) "" else "s"}")
        }
    }
}

@Composable
private fun QuizPerformanceCard(overview: AnalyticsOverview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Quiz performance", style = MaterialTheme.typography.titleMedium)
            if (overview.totalAttempts == 0) {
                Text(text = "No quiz attempts yet.")
            } else {
                Text(text = "Pass rate: ${formatPercent(overview.passRate)}")
                Text(text = "${overview.passedAttempts} / ${overview.totalAttempts} attempts passed")
                Text(text = "Average score: ${formatPercent(overview.averageScore)}")
            }
        }
    }
}

@Composable
private fun BreakBehaviorCard(stats: LocalStudyStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Break behavior", style = MaterialTheme.typography.titleMedium)
            Text(text = "Scheduled breaks taken: ${stats.scheduledBreaksTaken}")
            Text(text = "Unscheduled breaks taken: ${stats.unscheduledBreaksTaken}")
            if (stats.averageExtensionMillis > 0) {
                Text(text = "Average extension: ${formatDuration(stats.averageExtensionMillis)}")
            }
        }
    }
}

/**
 * Weak-topics section — gentle, non-judgmental framing per docs/PRODUCT.md
 * ("encouraging, non-judgmental copy") and docs/ANALYTICS.md's Dashboard
 * Composition ("A few topics could use another look").
 */
@Composable
private fun WeakTopicsCard(weakTopics: List<TopicPerformanceSummary>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "A few topics could use another look", style = MaterialTheme.typography.titleMedium)
            weakTopics.forEach { topic ->
                Text(text = weakTopicMessage(topic))
            }
        }
    }
}

@Composable
private fun TopicPerformanceRow(topic: TopicPerformanceSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = topic.topicName ?: "Topic")
            Text(text = "${formatPercent(topic.averageScore)} average across ${topic.attemptCount} attempt${if (topic.attemptCount == 1) "" else "s"}")
        }
    }
}

/** e.g. "Parsing — 61%. Revise Parsing before your next focus session." */
internal fun weakTopicMessage(topic: TopicPerformanceSummary): String {
    val name = topic.topicName ?: "This topic"
    return "$name — ${formatPercent(topic.averageScore)}. Revise $name before your next focus session."
}

internal fun formatPercent(fraction: Double): String {
    return "${(fraction * 100).roundToInt()}%"
}

internal fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        String.format(Locale.US, "%dh %dm", hours, minutes)
    } else {
        String.format(Locale.US, "%dm", minutes)
    }
}

package com.prove2break.ui.breaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * The real RAG-grounded rapid-fire verification quiz (QUIZ-SYSTEM.md,
 * UX-UI.md), replacing the Phase 5 `VerificationStubScreen` dev pass/fail
 * buttons entirely. One question at a time, full screen, a visible
 * per-question countdown, no back/skip/pause/re-answer — [QuizViewModel]
 * auto-advances on timeout exactly like a manual answer. On the last
 * question it submits to the backend and this same screen shows the scored
 * result using only non-judgmental copy (PRODUCT.md Product Principle #3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RapidFireQuizScreen(
    onResolved: (passed: Boolean, breakId: Long?) -> Unit,
    onError: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quick Check") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val current = state) {
                is QuizUiState.Loading -> {
                    CircularProgressIndicator()
                    Text(text = "Preparing your quick check...")
                }
                is QuizUiState.InProgress -> {
                    Text(
                        text = "Question ${current.questionNumber} of ${current.totalQuestions}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LinearProgressIndicator(
                        progress = { current.questionNumber.toFloat() / current.totalQuestions },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${current.secondsRemaining}s",
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = current.question.prompt,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    current.question.options.forEachIndexed { optionIndex, option ->
                        Button(
                            onClick = { viewModel.selectAnswer(optionIndex) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = option)
                        }
                    }
                }
                is QuizUiState.Submitting -> {
                    CircularProgressIndicator()
                    Text(text = "Scoring your answers...")
                }
                is QuizUiState.Result -> {
                    val percent = (current.score * 100).toInt()
                    if (current.passed) {
                        Text(text = "Nice work — break unlocked!", style = MaterialTheme.typography.titleLarge)
                        Text(text = "You got ${current.correctCount}/${current.totalQuestions} ($percent%).")
                    } else {
                        // Non-punitive copy per PRODUCT.md Product Principle #3 — never
                        // "failed", always framed as what to do next.
                        Text(text = "Break remains locked — keep studying and try again.", style = MaterialTheme.typography.titleLarge)
                        Text(text = "You got ${current.correctCount}/${current.totalQuestions} ($percent%). A little more review should get you there.")
                    }
                    Button(onClick = { onResolved(current.passed, current.breakId) }) {
                        Text(if (current.passed) "Start Break" else "Back to studying")
                    }
                }
                is QuizUiState.Error -> {
                    Text(text = current.message, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onError) {
                        Text("Back to studying")
                    }
                }
            }
        }
    }
}

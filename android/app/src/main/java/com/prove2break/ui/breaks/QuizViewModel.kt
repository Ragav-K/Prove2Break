package com.prove2break.ui.breaks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.breaks.BreakManager
import com.prove2break.data.repository.QuizRepository
import com.prove2break.domain.model.Question
import com.prove2break.domain.usecase.GenerateAndStartQuizUseCase
import com.prove2break.quiz.QuizRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data class InProgress(
        val question: Question,
        val questionNumber: Int,
        val totalQuestions: Int,
        val secondsRemaining: Int,
    ) : QuizUiState
    data object Submitting : QuizUiState
    data class Result(
        val passed: Boolean,
        val score: Double,
        val correctCount: Int,
        val totalQuestions: Int,
        val breakId: Long?,
    ) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

/**
 * Drives the rapid-fire quiz screen end to end (QUIZ-SYSTEM.md): generates +
 * starts an attempt for the claimed topics, runs the timed one-at-a-time
 * question loop via [QuizRunner] (no back/skip/pause/re-answer — see that
 * class), submits the recorded answers on the last question, and resolves the
 * real pass/fail into [BreakManager] (replacing the Phase 5 stub's dev
 * buttons) before exposing the non-judgmental [QuizUiState.Result].
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val generateAndStartQuiz: GenerateAndStartQuizUseCase,
    private val quizRepository: QuizRepository,
    private val breakManager: BreakManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    private val subjectId: String = savedStateHandle.get<String>("subjectId").orEmpty()
    private val topicIds: List<String> = savedStateHandle.get<String>("topicIds")
        .orEmpty()
        .split(",")
        .filter { it.isNotBlank() }

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var quizId: String? = null
    private var runner: QuizRunner? = null

    init {
        viewModelScope.launch {
            runCatching {
                generateAndStartQuiz(subjectId = subjectId, topicIds = topicIds)
            }.onSuccess { result ->
                quizId = result.quizId
                val questions = result.attempt.questions.sortedBy { it.orderIndex }
                if (questions.isEmpty()) {
                    _uiState.value = QuizUiState.Error("No questions were generated for these topics.")
                    return@onSuccess
                }
                runner = QuizRunner(questions)
                publishCurrentQuestion()
                runTicker()
            }.onFailure { error ->
                _uiState.value = QuizUiState.Error(error.message ?: "Couldn't start the quiz. Please try again.")
            }
        }
    }

    /** Student picked an option for the current question — no changing it afterward. */
    fun selectAnswer(optionIndex: Int) {
        val current = runner ?: return
        if (current.isComplete) return
        current.selectAnswer(optionIndex)
        advanceOrSubmit()
    }

    private fun runTicker() {
        viewModelScope.launch {
            val current = runner ?: return@launch
            while (!current.isComplete) {
                delay(1000L)
                current.tick()
                if (current.isComplete) {
                    submitAndResolve()
                    return@launch
                }
                publishCurrentQuestion()
            }
        }
    }

    private fun advanceOrSubmit() {
        val current = runner ?: return
        if (current.isComplete) {
            viewModelScope.launch { submitAndResolve() }
        } else {
            publishCurrentQuestion()
        }
    }

    private fun publishCurrentQuestion() {
        val current = runner ?: return
        val question = current.currentQuestion ?: return
        _uiState.value = QuizUiState.InProgress(
            question = question,
            questionNumber = current.currentIndex + 1,
            totalQuestions = current.totalQuestions,
            secondsRemaining = current.secondsRemainingOnCurrent,
        )
    }

    private suspend fun submitAndResolve() {
        val id = quizId ?: return
        val answers = runner?.recordedAnswers.orEmpty()
        _uiState.value = QuizUiState.Submitting
        runCatching { quizRepository.submitAnswers(id, answers) }
            .onSuccess { result ->
                val breakId = breakManager.resolveQuizResult(
                    sessionId = sessionId,
                    pass = result.passed,
                    breakLengthMillis = BreakManager.DEFAULT_UNSCHEDULED_BREAK_LENGTH_MILLIS,
                )
                _uiState.value = QuizUiState.Result(
                    passed = result.passed,
                    score = result.score,
                    correctCount = result.correctCount,
                    totalQuestions = result.totalQuestions,
                    breakId = breakId,
                )
            }
            .onFailure { error ->
                _uiState.value = QuizUiState.Error(error.message ?: "Couldn't submit the quiz. Please try again.")
            }
    }
}

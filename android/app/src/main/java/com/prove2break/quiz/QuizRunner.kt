package com.prove2break.quiz

import com.prove2break.domain.model.Question
import com.prove2break.domain.model.QuizAnswer

/**
 * Pure rapid-fire state machine (QUIZ-SYSTEM.md "Rapid-Fire Presentation" /
 * UX-UI.md): one question at a time, a per-question countdown driven by
 * external [tick] calls (so the caller — a ViewModel coroutine — owns the
 * real-time ticking; this class has no timers or dispatchers of its own,
 * which is what makes it directly unit-testable), no back/skip/pause, and
 * exactly one [QuizAnswer] recorded per [Question] in order — a timeout
 * records `selectedOptionIndex = null` (QUIZ-SYSTEM.md "Scoring": counted as
 * incorrect) and auto-advances.
 */
class QuizRunner(private val questions: List<Question>) {

    private var index = 0
    private var secondsRemaining = questions.firstOrNull()?.timeLimitSeconds ?: 0
    private var elapsedInQuestion = 0
    private val answers = mutableListOf<QuizAnswer>()

    val isComplete: Boolean get() = index >= questions.size
    val currentQuestion: Question? get() = questions.getOrNull(index)
    val currentIndex: Int get() = index
    val totalQuestions: Int get() = questions.size
    val secondsRemainingOnCurrent: Int get() = secondsRemaining
    val recordedAnswers: List<QuizAnswer> get() = answers.toList()

    /** Student picked an option for the current question — records it and advances immediately. */
    fun selectAnswer(optionIndex: Int) {
        if (isComplete) return
        recordAndAdvance(optionIndex)
    }

    /**
     * One second elapsed. Returns true if this tick caused an automatic
     * (timeout) advance to the next question — the caller can use this to
     * distinguish "still counting down" from "auto-advanced" for analytics/UX,
     * though both cases update the same observable state.
     */
    fun tick(): Boolean {
        if (isComplete) return false
        secondsRemaining -= 1
        elapsedInQuestion += 1
        if (secondsRemaining <= 0) {
            recordAndAdvance(null)
            return true
        }
        return false
    }

    private fun recordAndAdvance(selectedOptionIndex: Int?) {
        val question = questions[index]
        answers += QuizAnswer(
            questionId = question.id,
            selectedOptionIndex = selectedOptionIndex,
            timeTakenSeconds = elapsedInQuestion.toDouble(),
        )
        index += 1
        elapsedInQuestion = 0
        secondsRemaining = questions.getOrNull(index)?.timeLimitSeconds ?: 0
    }
}

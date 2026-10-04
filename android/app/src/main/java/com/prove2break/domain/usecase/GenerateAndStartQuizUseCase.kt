package com.prove2break.domain.usecase

import com.prove2break.data.repository.QuizRepository
import com.prove2break.domain.model.QuizAttemptStart
import javax.inject.Inject

/**
 * Orchestrates `POST /api/quiz/generate` followed by `POST /api/quiz/{id}/attempt`
 * (QUIZ-SYSTEM.md "Topic Selection -> Generation Flow") so the quiz UI only has to
 * deal with one call: hand it claimed topics, get back a ready-to-answer attempt.
 */
class GenerateAndStartQuizUseCase @Inject constructor(
    private val repository: QuizRepository,
) {
    data class Result(
        val quizId: String,
        val attempt: QuizAttemptStart,
        val passThresholdUsed: Double,
    )

    suspend operator fun invoke(
        subjectId: String,
        topicIds: List<String>,
        questionCount: Int = DEFAULT_QUESTION_COUNT,
        secondsPerQuestion: Int = DEFAULT_SECONDS_PER_QUESTION,
    ): Result {
        require(topicIds.isNotEmpty()) { "At least one topic must be selected" }
        val quiz = repository.generateQuiz(subjectId, topicIds, questionCount, secondsPerQuestion)
        val attempt = repository.startAttempt(quiz.id)
        return Result(quizId = quiz.id, attempt = attempt, passThresholdUsed = quiz.passThresholdUsed)
    }

    companion object {
        const val DEFAULT_QUESTION_COUNT = 5
        const val DEFAULT_SECONDS_PER_QUESTION = 15
    }
}

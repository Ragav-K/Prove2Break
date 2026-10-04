package com.prove2break.domain.model

/** One rapid-fire quiz question, answer key stripped (QUIZ-SYSTEM.md "Question JSON Schema"). */
data class Question(
    val id: String,
    val topicId: String,
    val prompt: String,
    val options: List<String>,
    val timeLimitSeconds: Int,
    val orderIndex: Int,
)

/** A generated quiz batch, before an attempt has been started. */
data class Quiz(
    val id: String,
    val subjectId: String,
    val passThresholdUsed: Double,
    val questions: List<Question>,
)

/** Result of `POST /api/quiz/{id}/attempt` — the same questions, tied to an attempt. */
data class QuizAttemptStart(
    val attemptId: String,
    val quizId: String,
    val questions: List<Question>,
)

/** One answer the student gave (or a timeout, `selectedOptionIndex = null`) for one question. */
data class QuizAnswer(
    val questionId: String,
    val selectedOptionIndex: Int?,
    val timeTakenSeconds: Double?,
)

/** Scored outcome of a submitted or completed quiz attempt. */
data class QuizResult(
    val attemptId: String,
    val quizId: String,
    val score: Double,
    val passed: Boolean,
    val correctCount: Int,
    val totalQuestions: Int,
)

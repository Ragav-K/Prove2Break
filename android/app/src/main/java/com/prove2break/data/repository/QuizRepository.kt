package com.prove2break.data.repository

import com.prove2break.domain.model.Quiz
import com.prove2break.domain.model.QuizAnswer
import com.prove2break.domain.model.QuizAttemptStart
import com.prove2break.domain.model.QuizResult

/** Backend-backed source for the RAG-grounded rapid-fire quiz (QUIZ-SYSTEM.md). */
interface QuizRepository {
    suspend fun generateQuiz(
        subjectId: String,
        topicIds: List<String>,
        questionCount: Int,
        secondsPerQuestion: Int,
    ): Quiz

    suspend fun startAttempt(quizId: String): QuizAttemptStart

    suspend fun submitAnswers(quizId: String, answers: List<QuizAnswer>): QuizResult

    suspend fun getResult(quizId: String): QuizResult
}

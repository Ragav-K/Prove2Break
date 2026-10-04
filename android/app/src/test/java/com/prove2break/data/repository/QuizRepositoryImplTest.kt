package com.prove2break.data.repository

import com.prove2break.data.remote.dto.AttemptStartResponseDto
import com.prove2break.data.remote.dto.QuestionPublicDto
import com.prove2break.data.remote.dto.QuizResponseDto
import com.prove2break.data.remote.dto.QuizResultResponseDto
import com.prove2break.data.remote.dto.QuizSubmitResponseDto
import com.prove2break.domain.model.QuizAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** DTO <-> domain mapping, matching backend/app/schemas/quiz.py field-for-field. */
class QuizRepositoryImplTest {

    private fun questionDto(id: String, orderIndex: Int) = QuestionPublicDto(
        id = id,
        topicId = "topic-1",
        prompt = "Prompt $orderIndex",
        options = listOf("A", "B", "C", "D"),
        timeLimitSeconds = 15,
        orderIndex = orderIndex,
    )

    @Test
    fun `QuizResponseDto maps to domain and sorts questions by order index`() {
        val dto = QuizResponseDto(
            id = "quiz-1",
            subjectId = "subj-1",
            passThresholdUsed = 0.7,
            createdAt = "2026-01-01T00:00:00Z",
            questions = listOf(questionDto("q2", 1), questionDto("q1", 0)),
        )

        val domain = dto.toDomain()

        assertEquals("quiz-1", domain.id)
        assertEquals("subj-1", domain.subjectId)
        assertEquals(0.7, domain.passThresholdUsed, 0.0001)
        assertEquals(listOf("q1", "q2"), domain.questions.map { it.id })
    }

    @Test
    fun `AttemptStartResponseDto maps attempt and quiz ids and questions`() {
        val dto = AttemptStartResponseDto(
            attemptId = "attempt-1",
            quizId = "quiz-1",
            questions = listOf(questionDto("q1", 0)),
        )

        val domain = dto.toDomain()

        assertEquals("attempt-1", domain.attemptId)
        assertEquals("quiz-1", domain.quizId)
        assertEquals(1, domain.questions.size)
        assertEquals("q1", domain.questions.first().id)
    }

    @Test
    fun `an unanswered question maps to a null selectedOptionIndex both ways`() {
        val answer = QuizAnswer(questionId = "q1", selectedOptionIndex = null, timeTakenSeconds = 15.0)

        val dto = answer.toDto()

        assertEquals("q1", dto.questionId)
        assertNull(dto.selectedOptionIndex)
        assertEquals(15.0, dto.timeTakenSeconds!!, 0.0001)
    }

    @Test
    fun `QuizSubmitResponseDto maps to domain, threading through the caller's known quizId`() {
        val dto = QuizSubmitResponseDto(
            attemptId = "attempt-1",
            score = 0.8,
            passed = true,
            correctCount = 4,
            totalQuestions = 5,
        )

        val domain = dto.toDomain(quizId = "quiz-1")

        assertEquals("attempt-1", domain.attemptId)
        assertEquals("quiz-1", domain.quizId)
        assertEquals(0.8, domain.score, 0.0001)
        assertEquals(true, domain.passed)
        assertEquals(4, domain.correctCount)
        assertEquals(5, domain.totalQuestions)
    }

    @Test
    fun `QuizResultResponseDto maps every field to domain`() {
        val dto = QuizResultResponseDto(
            attemptId = "attempt-1",
            quizId = "quiz-1",
            score = 0.6,
            passed = false,
            correctCount = 3,
            totalQuestions = 5,
            completedAt = "2026-01-01T00:05:00Z",
        )

        val domain = dto.toDomain()

        assertEquals("attempt-1", domain.attemptId)
        assertEquals("quiz-1", domain.quizId)
        assertEquals(0.6, domain.score, 0.0001)
        assertEquals(false, domain.passed)
        assertEquals(3, domain.correctCount)
        assertEquals(5, domain.totalQuestions)
    }
}

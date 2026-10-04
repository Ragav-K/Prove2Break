package com.prove2break.data.repository

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.AnswerSubmitDto
import com.prove2break.data.remote.dto.AttemptStartResponseDto
import com.prove2break.data.remote.dto.QuestionPublicDto
import com.prove2break.data.remote.dto.QuizGenerateRequest
import com.prove2break.data.remote.dto.QuizResponseDto
import com.prove2break.data.remote.dto.QuizResultResponseDto
import com.prove2break.data.remote.dto.QuizSubmitRequestDto
import com.prove2break.data.remote.dto.QuizSubmitResponseDto
import com.prove2break.domain.model.Question
import com.prove2break.domain.model.Quiz
import com.prove2break.domain.model.QuizAnswer
import com.prove2break.domain.model.QuizAttemptStart
import com.prove2break.domain.model.QuizResult
import javax.inject.Inject

class QuizRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
) : QuizRepository {

    override suspend fun generateQuiz(
        subjectId: String,
        topicIds: List<String>,
        questionCount: Int,
        secondsPerQuestion: Int,
    ): Quiz {
        val response = api.generateQuiz(
            QuizGenerateRequest(
                subjectId = subjectId,
                topicIds = topicIds,
                numQuestions = questionCount,
                timeLimitSeconds = secondsPerQuestion,
            ),
        )
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to generate quiz")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun startAttempt(quizId: String): QuizAttemptStart {
        val response = api.startQuizAttempt(quizId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to start quiz attempt")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun submitAnswers(quizId: String, answers: List<QuizAnswer>): QuizResult {
        val response = api.submitQuiz(
            quizId,
            QuizSubmitRequestDto(answers = answers.map { it.toDto() }),
        )
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to submit quiz")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain(quizId)
    }

    override suspend fun getResult(quizId: String): QuizResult {
        val response = api.getQuizResult(quizId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load quiz result")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }
}

internal fun QuestionPublicDto.toDomain(): Question = Question(
    id = id,
    topicId = topicId,
    prompt = prompt,
    options = options,
    timeLimitSeconds = timeLimitSeconds,
    orderIndex = orderIndex,
)

internal fun QuizResponseDto.toDomain(): Quiz = Quiz(
    id = id,
    subjectId = subjectId,
    passThresholdUsed = passThresholdUsed,
    questions = questions.map { it.toDomain() }.sortedBy { it.orderIndex },
)

internal fun AttemptStartResponseDto.toDomain(): QuizAttemptStart = QuizAttemptStart(
    attemptId = attemptId,
    quizId = quizId,
    questions = questions.map { it.toDomain() }.sortedBy { it.orderIndex },
)

internal fun QuizAnswer.toDto(): AnswerSubmitDto = AnswerSubmitDto(
    questionId = questionId,
    selectedOptionIndex = selectedOptionIndex,
    timeTakenSeconds = timeTakenSeconds,
)

/** [QuizSubmitResponseDto] has no `quiz_id` field, so the caller's known id is threaded through. */
internal fun QuizSubmitResponseDto.toDomain(quizId: String): QuizResult = QuizResult(
    attemptId = attemptId,
    quizId = quizId,
    score = score,
    passed = passed,
    correctCount = correctCount,
    totalQuestions = totalQuestions,
)

internal fun QuizResultResponseDto.toDomain(): QuizResult = QuizResult(
    attemptId = attemptId,
    quizId = quizId,
    score = score,
    passed = passed,
    correctCount = correctCount,
    totalQuestions = totalQuestions,
)

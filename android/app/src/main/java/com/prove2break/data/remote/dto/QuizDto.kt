package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/schemas/quiz.py QuizGenerateRequest. */
data class QuizGenerateRequest(
    @SerializedName("subject_id") val subjectId: String,
    @SerializedName("topic_ids") val topicIds: List<String>,
    @SerializedName("num_questions") val numQuestions: Int = 5,
    @SerializedName("time_limit_seconds") val timeLimitSeconds: Int = 15,
)

/**
 * Mirrors backend/app/schemas/quiz.py QuestionPublic — served with
 * `correct_option_index` and `source_chunk_ids` stripped server-side
 * (QUIZ-SYSTEM.md "Question JSON Schema").
 */
data class QuestionPublicDto(
    @SerializedName("id") val id: String,
    @SerializedName("topic_id") val topicId: String,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("time_limit_seconds") val timeLimitSeconds: Int,
    @SerializedName("order_index") val orderIndex: Int,
)

/** Mirrors backend/app/schemas/quiz.py QuizResponse. */
data class QuizResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("subject_id") val subjectId: String,
    @SerializedName("pass_threshold_used") val passThresholdUsed: Double,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("questions") val questions: List<QuestionPublicDto>,
)

/** Mirrors backend/app/schemas/quiz.py AttemptStartResponse. */
data class AttemptStartResponseDto(
    @SerializedName("attempt_id") val attemptId: String,
    @SerializedName("quiz_id") val quizId: String,
    @SerializedName("questions") val questions: List<QuestionPublicDto>,
)

/** Mirrors backend/app/schemas/quiz.py AnswerSubmit. */
data class AnswerSubmitDto(
    @SerializedName("question_id") val questionId: String,
    @SerializedName("selected_option_index") val selectedOptionIndex: Int?,
    @SerializedName("time_taken_seconds") val timeTakenSeconds: Double?,
)

/** Mirrors backend/app/schemas/quiz.py QuizSubmitRequest. */
data class QuizSubmitRequestDto(
    @SerializedName("answers") val answers: List<AnswerSubmitDto>,
)

/** Mirrors backend/app/schemas/quiz.py QuizSubmitResponse. */
data class QuizSubmitResponseDto(
    @SerializedName("attempt_id") val attemptId: String,
    @SerializedName("score") val score: Double,
    @SerializedName("passed") val passed: Boolean,
    @SerializedName("correct_count") val correctCount: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
)

/** Mirrors backend/app/schemas/quiz.py QuizResultResponse. */
data class QuizResultResponseDto(
    @SerializedName("attempt_id") val attemptId: String,
    @SerializedName("quiz_id") val quizId: String,
    @SerializedName("score") val score: Double,
    @SerializedName("passed") val passed: Boolean,
    @SerializedName("correct_count") val correctCount: Int,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("completed_at") val completedAt: String?,
)

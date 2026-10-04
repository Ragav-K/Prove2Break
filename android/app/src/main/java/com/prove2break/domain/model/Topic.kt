package com.prove2break.domain.model

enum class TopicCompletionStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

enum class TopicDifficulty { EASY, MEDIUM, HARD }

data class Topic(
    val id: String,
    val subjectId: String,
    val unitId: String?,
    val name: String,
    val description: String?,
    val sourceReference: String?,
    val completionStatus: TopicCompletionStatus,
    val difficulty: TopicDifficulty,
    val performance: Double?,
) {
    val isCompleted: Boolean get() = completionStatus == TopicCompletionStatus.COMPLETED
}

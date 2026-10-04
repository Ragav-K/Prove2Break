package com.prove2break.data.repository

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.TopicCompletionStatusDto
import com.prove2break.data.remote.dto.TopicDifficultyDto
import com.prove2break.data.remote.dto.TopicResponseDto
import com.prove2break.data.remote.dto.TopicUpdateRequest
import com.prove2break.domain.model.Topic
import com.prove2break.domain.model.TopicCompletionStatus
import com.prove2break.domain.model.TopicDifficulty
import javax.inject.Inject

class TopicRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
) : TopicRepository {

    override suspend fun listTopics(subjectId: String): List<Topic> {
        val response = api.listTopics(subjectId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load topics")
        return response.body().orEmpty().map { it.toDomain() }
    }

    override suspend fun renameTopic(topicId: String, name: String): Topic {
        val response = api.updateTopic(topicId, TopicUpdateRequest(name = name))
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to rename topic")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun setTopicCompleted(topicId: String, isCompleted: Boolean): Topic {
        val response = api.updateTopic(topicId, TopicUpdateRequest(isCompleted = isCompleted))
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to update topic")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }
}

internal fun TopicResponseDto.toDomain(): Topic = Topic(
    id = id,
    subjectId = subjectId,
    unitId = unitId,
    name = name,
    description = description,
    sourceReference = sourceReference,
    completionStatus = completionStatus.toDomain(),
    difficulty = difficulty.toDomain(),
    performance = performance,
)

internal fun TopicCompletionStatusDto.toDomain(): TopicCompletionStatus = when (this) {
    TopicCompletionStatusDto.NOT_STARTED -> TopicCompletionStatus.NOT_STARTED
    TopicCompletionStatusDto.IN_PROGRESS -> TopicCompletionStatus.IN_PROGRESS
    TopicCompletionStatusDto.COMPLETED -> TopicCompletionStatus.COMPLETED
}

internal fun TopicDifficultyDto.toDomain(): TopicDifficulty = when (this) {
    TopicDifficultyDto.EASY -> TopicDifficulty.EASY
    TopicDifficultyDto.MEDIUM -> TopicDifficulty.MEDIUM
    TopicDifficultyDto.HARD -> TopicDifficulty.HARD
}

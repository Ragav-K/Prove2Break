package com.prove2break.data.repository

import com.prove2break.domain.model.Topic

interface TopicRepository {
    suspend fun listTopics(subjectId: String): List<Topic>
    suspend fun renameTopic(topicId: String, name: String): Topic
    suspend fun setTopicCompleted(topicId: String, isCompleted: Boolean): Topic
}

package com.prove2break.domain.usecase

import com.prove2break.data.repository.TopicRepository
import com.prove2break.domain.model.Topic
import javax.inject.Inject

class RenameTopicUseCase @Inject constructor(
    private val repository: TopicRepository,
) {
    suspend operator fun invoke(topicId: String, newName: String): Topic {
        require(newName.isNotBlank()) { "Topic name cannot be blank" }
        return repository.renameTopic(topicId, newName.trim())
    }
}

package com.prove2break.domain.usecase

import com.prove2break.data.repository.TopicRepository
import com.prove2break.domain.model.Topic
import javax.inject.Inject

class ToggleTopicCompletionUseCase @Inject constructor(
    private val repository: TopicRepository,
) {
    suspend operator fun invoke(topic: Topic): Topic {
        return repository.setTopicCompleted(topic.id, !topic.isCompleted)
    }
}

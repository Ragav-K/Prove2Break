package com.prove2break.domain.usecase

import com.prove2break.data.repository.TopicRepository
import com.prove2break.domain.model.Topic
import com.prove2break.domain.model.TopicCompletionStatus
import com.prove2break.domain.model.TopicDifficulty
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeTopicRepository : TopicRepository {
    var lastTopicId: String? = null
    var lastIsCompleted: Boolean? = null

    override suspend fun listTopics(subjectId: String): List<Topic> = emptyList()

    override suspend fun renameTopic(topicId: String, name: String): Topic {
        throw UnsupportedOperationException()
    }

    override suspend fun setTopicCompleted(topicId: String, isCompleted: Boolean): Topic {
        lastTopicId = topicId
        lastIsCompleted = isCompleted
        return Topic(
            id = topicId,
            subjectId = "subj-1",
            unitId = null,
            name = "Topic",
            description = null,
            sourceReference = null,
            completionStatus = if (isCompleted) TopicCompletionStatus.COMPLETED else TopicCompletionStatus.NOT_STARTED,
            difficulty = TopicDifficulty.MEDIUM,
            performance = null,
        )
    }
}

class ToggleTopicCompletionUseCaseTest {

    private fun topic(status: TopicCompletionStatus) = Topic(
        id = "topic-1",
        subjectId = "subj-1",
        unitId = null,
        name = "Photosynthesis",
        description = null,
        sourceReference = null,
        completionStatus = status,
        difficulty = TopicDifficulty.MEDIUM,
        performance = null,
    )

    @Test
    fun `toggling a not-started topic marks it completed`() = runBlocking {
        val repository = FakeTopicRepository()
        val useCase = ToggleTopicCompletionUseCase(repository)

        val result = useCase(topic(TopicCompletionStatus.NOT_STARTED))

        assertEquals(true, repository.lastIsCompleted)
        assertEquals(TopicCompletionStatus.COMPLETED, result.completionStatus)
    }

    @Test
    fun `toggling a completed topic marks it not completed`() = runBlocking {
        val repository = FakeTopicRepository()
        val useCase = ToggleTopicCompletionUseCase(repository)

        val result = useCase(topic(TopicCompletionStatus.COMPLETED))

        assertEquals(false, repository.lastIsCompleted)
        assertEquals(TopicCompletionStatus.NOT_STARTED, result.completionStatus)
    }

    @Test
    fun `toggling an in-progress topic is treated as not-yet-completed and marks it completed`() = runBlocking {
        val repository = FakeTopicRepository()
        val useCase = ToggleTopicCompletionUseCase(repository)

        useCase(topic(TopicCompletionStatus.IN_PROGRESS))

        assertEquals(true, repository.lastIsCompleted)
    }
}

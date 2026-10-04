package com.prove2break.quiz

import com.prove2break.domain.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizRunnerTest {

    private fun question(id: String, orderIndex: Int, timeLimitSeconds: Int = 15) = Question(
        id = id,
        topicId = "topic-1",
        prompt = "Prompt $orderIndex",
        options = listOf("A", "B", "C", "D"),
        timeLimitSeconds = timeLimitSeconds,
        orderIndex = orderIndex,
    )

    @Test
    fun `selecting an answer records it and immediately advances`() {
        val questions = listOf(question("q1", 0), question("q2", 1))
        val runner = QuizRunner(questions)

        assertEquals("q1", runner.currentQuestion?.id)
        runner.selectAnswer(2)

        assertEquals("q2", runner.currentQuestion?.id)
        assertEquals(1, runner.recordedAnswers.size)
        assertEquals("q1", runner.recordedAnswers[0].questionId)
        assertEquals(2, runner.recordedAnswers[0].selectedOptionIndex)
    }

    @Test
    fun `timeout with no answer selected records a null selection and auto-advances`() {
        val questions = listOf(question("q1", 0, timeLimitSeconds = 3), question("q2", 1))
        val runner = QuizRunner(questions)

        var autoAdvanced = false
        repeat(3) { autoAdvanced = runner.tick() }

        assertTrue("tick() should report an auto-advance once the timer hits zero", autoAdvanced)
        assertEquals(1, runner.recordedAnswers.size)
        assertEquals("q1", runner.recordedAnswers[0].questionId)
        assertNull(runner.recordedAnswers[0].selectedOptionIndex)
        assertEquals("q2", runner.currentQuestion?.id)
    }

    @Test
    fun `ticking before the timer expires does not advance or record anything`() {
        val questions = listOf(question("q1", 0, timeLimitSeconds = 5))
        val runner = QuizRunner(questions)

        val advanced = runner.tick()

        assertEquals(false, advanced)
        assertEquals(4, runner.secondsRemainingOnCurrent)
        assertTrue(runner.recordedAnswers.isEmpty())
        assertEquals("q1", runner.currentQuestion?.id)
    }

    @Test
    fun `completing the last question marks the runner complete with one answer per question in order`() {
        val questions = listOf(question("q1", 0, timeLimitSeconds = 2), question("q2", 1, timeLimitSeconds = 2))
        val runner = QuizRunner(questions)

        runner.selectAnswer(0)
        assertTrue(!runner.isComplete)
        runner.selectAnswer(3)

        assertTrue(runner.isComplete)
        assertNull(runner.currentQuestion)
        assertEquals(2, runner.recordedAnswers.size)
        assertEquals(listOf("q1", "q2"), runner.recordedAnswers.map { it.questionId })
        assertEquals(listOf(0, 3), runner.recordedAnswers.map { it.selectedOptionIndex })
    }

    @Test
    fun `once complete, further selectAnswer and tick calls are no-ops`() {
        val questions = listOf(question("q1", 0, timeLimitSeconds = 1))
        val runner = QuizRunner(questions)

        runner.selectAnswer(1)
        assertTrue(runner.isComplete)

        val advancedAgain = runner.tick()
        runner.selectAnswer(2)

        assertEquals(false, advancedAgain)
        assertEquals(1, runner.recordedAnswers.size)
    }
}

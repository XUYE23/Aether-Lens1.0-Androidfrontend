package com.aether.app.workspace

import app.cash.turbine.test
import com.aether.app.data.MockData
import com.aether.app.voice.MockScript
import com.aether.app.voice.MockVoiceRecognitionManager
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WorkspaceCardStateHolderTest {

    private fun makeHolder(
        script: MockScript = MockScript.Success,
        scope: kotlinx.coroutines.CoroutineScope,
        onExecuteCommand: (String) -> Unit = {}
    ): Pair<WorkspaceCardStateHolder, MockVoiceRecognitionManager> {
        val mock = MockVoiceRecognitionManager(script, scope = scope)
        val holder = WorkspaceCardStateHolder(
            voiceManager = mock,
            scope = scope,
            onExecuteCommand = onExecuteCommand,
            initialCards = MockData.getCardList()
        )
        return holder to mock
    }

    @Test
    fun `initial state is Idle with null draft`() = runTest {
        val (holder, _) = makeHolder(scope = this)
        assertEquals(VoicePhase.Idle, holder.phase.value)
        assertNull(holder.draftVoiceCard.value)
    }

    @Test
    fun `press start transitions phase to Listening`() = runTest {
        val (holder, _) = makeHolder(scope = this)
        holder.phase.test {
            assertEquals(VoicePhase.Idle, awaitItem())
            holder.onHaloPressStart()
            assertEquals(VoicePhase.Listening, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNotNull(holder.draftVoiceCard.value)
    }

    @Test
    fun `happy path - Listening transitions to Editable with finalText`() = runTest {
        val (holder, _) = makeHolder(MockScript.Success, scope = this)
        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()

        assertEquals(VoicePhase.Editable, holder.phase.value)
        assertEquals("提醒张三下午三点开会", holder.draftVoiceCard.value?.finalText)
    }

    @Test
    fun `empty result transitions to Dismissing without red text`() = runTest {
        val (holder, _) = makeHolder(MockScript.Empty, scope = this)
        holder.onHaloPressStart()
        advanceTimeBy(2_000)
        holder.phase.test {
            skipItems(1)
            holder.onHaloPressEnd()
            advanceUntilIdle()
            assertEquals(VoicePhase.Dismissing, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(holder.draftVoiceCard.value?.errorMessage)
    }

    @Test
    fun `network error transitions to Error then auto-Dismissing after 1500ms`() = runTest {
        val (holder, _) = makeHolder(MockScript.NetworkError, scope = this)
        holder.onHaloPressStart()
        advanceTimeBy(2_000)
        advanceUntilIdle()
        assertEquals(VoicePhase.Error, holder.phase.value)
        assertEquals("识别失败", holder.draftVoiceCard.value?.errorMessage)

        advanceTimeBy(1_600)
        advanceUntilIdle()
        assertEquals(VoicePhase.Dismissing, holder.phase.value)
    }

    @Test
    fun `right swipe calls executeCommand and moves to Dismissing`() = runTest {
        var executedText: String? = null
        val (holder, _) = makeHolder(MockScript.Success, scope = this,
            onExecuteCommand = { executedText = it })
        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()
        assertEquals(VoicePhase.Editable, holder.phase.value)

        holder.onDraftSwipedRight()
        assertEquals("提醒张三下午三点开会", executedText)
        assertEquals(VoicePhase.Dismissing, holder.phase.value)
    }

    @Test
    fun `left swipe does NOT call executeCommand and moves to Dismissing`() = runTest {
        var executedText: String? = null
        val (holder, _) = makeHolder(MockScript.Success, scope = this,
            onExecuteCommand = { executedText = it })
        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()

        holder.onDraftSwipedLeft()
        assertNull(executedText)
        assertEquals(VoicePhase.Dismissing, holder.phase.value)
    }

    @Test
    fun `onDismissComplete resets to Idle with null draft`() = runTest {
        val (holder, _) = makeHolder(MockScript.Success, scope = this)
        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()
        holder.onDraftSwipedRight()

        holder.onDismissComplete()
        assertEquals(VoicePhase.Idle, holder.phase.value)
        assertNull(holder.draftVoiceCard.value)
    }

    @Test
    fun `cardStack is unchanged throughout the entire voice flow`() = runTest {
        val (holder, _) = makeHolder(MockScript.Success, scope = this)
        val initialStack = holder.cardStack.value

        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()
        holder.onDraftSwipedRight()
        holder.onDismissComplete()

        assertEquals(initialStack, holder.cardStack.value)
    }

    @Test
    fun `text edit updates draftVoiceCard finalText`() = runTest {
        val (holder, _) = makeHolder(MockScript.Success, scope = this)
        holder.onHaloPressStart()
        advanceTimeBy(3_500)
        holder.onHaloPressEnd()
        advanceUntilIdle()

        holder.onDraftTextEdited("修改后的文字")
        assertEquals("修改后的文字", holder.draftVoiceCard.value?.finalText)
    }
}

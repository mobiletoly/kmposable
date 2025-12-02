package dev.goquick.kmposable.core

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow

class ParentChildStateTest {

    @Test
    fun mirrorChildState_updates_parent_immediately_and_on_changes() = runTest {
        val child = MutableStateFlow(0)
        val node = TestParentNode(parentScope = this)

        val job = node.mirrorChildState(child) { parent, childValue ->
            parent.copy(childSnapshot = childValue)
        }

        // Initial value mirrored
        assertEquals(0, node.state.value.childSnapshot)

        // Update child, expect parent to update
        child.value = 42
        advanceUntilIdle()
        assertEquals(42, node.state.value.childSnapshot)

        job.cancel()
        node.onDetach()
    }
}

private data class ParentState(val childSnapshot: Int = -1)

private class TestParentNode(parentScope: kotlinx.coroutines.CoroutineScope) :
    StatefulNode<ParentState, Nothing, Nothing>(
        parentScope = parentScope,
        initialState = ParentState(childSnapshot = 0)
    ) {
    override fun onEvent(event: Nothing) = Unit
}

package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.StatefulNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private class RootNode : Node<Unit, Unit, Unit> {
    private val _state = MutableStateFlow(Unit)
    override val state: StateFlow<Unit> = _state
    override fun onEvent(event: Unit) = Unit
    override val outputs = emptyFlow<Unit>()
}

private class ResultTestNode(
    scope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<Unit, Unit, Unit>(scope, Unit), ResultNode<String> {
    private val _result = MutableSharedFlow<KmposableResult<String>>(replay = 1, extraBufferCapacity = 1)
    override val result = _result

    override fun onEvent(event: Unit) = Unit

    suspend fun emitResult(value: String) {
        _result.emit(KmposableResult.Ok(value))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PushForResultTest {

    @Test
    fun returnsResultAndPopsEntry() = runTest {
        val navigator = KmposableStackNavigator(DefaultStackEntry(RootNode()))
        val resultNode = ResultTestNode(this)

        val deferred = async {
            navigator.pushForResult<Unit, DefaultStackEntry<Unit>, String> {
                DefaultStackEntry(resultNode)
            }
        }

        advanceUntilIdle()
        resultNode.emitResult("value")

        val result = deferred.await()
        assertIs<KmposableResult.Ok<String>>(result)
        assertEquals("value", result.value)
        assertEquals(1, navigator.state.value.stack.size) // root only
        resultNode.onDetach()
    }

    @Test
    fun returnsCanceledWhenEntryRemoved() = runTest {
        val navigator = KmposableStackNavigator(DefaultStackEntry(RootNode()))
        val resultNode = ResultTestNode(this)

        val deferred = async {
            navigator.pushForResult<Unit, DefaultStackEntry<Unit>, String> {
                DefaultStackEntry(resultNode)
            }
        }

        // Remove the entry before it emits.
        // pushForResult already pushed the entry; popping should trigger cancel path.
        // Ensure pop executes after pushForResult starts.
        advanceUntilIdle()
        navigator.pop()

        val result = deferred.await()
        assertEquals(KmposableResult.Canceled, result)
        resultNode.onDetach()
    }

    @Test
    fun throwsWhenNodeNotResultNode() = runTest {
        val navigator = KmposableStackNavigator(DefaultStackEntry(RootNode()))
        assertFailsWith<IllegalArgumentException> {
            navigator.pushForResult<Unit, DefaultStackEntry<Unit>, String> {
                DefaultStackEntry(RootNode())
            }
        }
    }
}

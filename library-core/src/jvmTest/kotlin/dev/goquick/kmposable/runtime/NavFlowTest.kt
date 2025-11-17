package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class NavFlowTest {

    @Test
    fun currentTopNodeReflectsNavigatorStack() {
        TestScope().runTest {
            val root = TestNode("root")
            val runtime = RecordingRuntime(this, root)
            try {
                runtime.start()
                advanceUntilIdle()
                val details = TestNode("details")

                runtime.push(details)
                advanceUntilIdle()

                assertSame(details, runtime.currentTopNode())
            } finally {
                runtime.dispose()
            }
        }
    }

    @Test
    fun outputsAreForwardedToRuntimeHook() {
        TestScope().runTest {
            val root = TestNode("root")
            val runtime = RecordingRuntime(this, root)
            try {
                runtime.start()
                advanceUntilIdle()
                root.emitOutput("open-list")
                advanceUntilIdle()

                assertEquals(listOf(root to "open-list"), runtime.recordedOutputs.toList())
            } finally {
                runtime.dispose()
            }
        }
    }

    @Test
    fun lifecycleAwareNodesReceiveCallbacks() {
        TestScope().runTest {
            val root = LifecycleAwareTestNode("root")
            val runtime = object : NavFlow<Unit, DefaultStackEntry<Unit>>(this, root) {}
            val detail = LifecycleAwareTestNode("detail")
            try {
                runtime.start()
                advanceUntilIdle()
                assertEquals(1, root.attachCount)

                runtime.push(detail)
                advanceUntilIdle()

                assertEquals(1, detail.attachCount)
                runtime.pop()
                advanceUntilIdle()

                assertEquals(1, detail.detachCount)
            } finally {
                runtime.dispose()
            }
        }
    }

    @Test
    fun replaceTopDetachesPreviousNode() = runTest {
        val root = TrackingNode("root")
        val runtime = object : NavFlow<Unit, DefaultStackEntry<Unit>>(this, root) {}
        val detail = TrackingNode("detail")
        val edit = TrackingNode("edit")
        runtime.start()
        runtime.push(detail)
        runtime.replaceTop(edit)

        assertEquals(1, detail.detachCount)
        assertEquals(1, edit.attachCount)
        runtime.dispose()
    }

    @Test
    fun popToRootDetachesIntermediateNodes() = runTest {
        val root = TrackingNode("root")
        val runtime = object : NavFlow<Unit, DefaultStackEntry<Unit>>(this, root) {}
        val list = TrackingNode("list")
        val detail = TrackingNode("detail")
        runtime.start()
        runtime.push(list)
        runtime.push(detail)

        runtime.popToRoot()

        assertEquals(1, detail.detachCount)
        assertEquals(1, list.detachCount)
        runtime.dispose()
    }

    @Test
    fun popAllReturnsToSingleNode() = runTest {
        val root = TrackingNode("root")
        val runtime = object : NavFlow<Unit, DefaultStackEntry<Unit>>(this, root) {}
        val list = TrackingNode("list")
        val detail = TrackingNode("detail")
        runtime.start()
        runtime.push(list)
        runtime.push(detail)

        runtime.popAll()

        assertEquals(1, list.detachCount)
        assertEquals(1, detail.detachCount)
        assertSame(root, runtime.currentTopNode())
        runtime.dispose()
    }

    private class RecordingRuntime(
        scope: CoroutineScope,
        root: Node<*, *, String>
    ) : NavFlow<String, DefaultStackEntry<String>>(scope, root) {
        val recordedOutputs = mutableListOf<Pair<Node<*, *, String>, String>>()
        override fun onNodeOutput(node: Node<*, *, String>, output: String) {
            recordedOutputs += node to output
        }
    }

    private class TestNode(
        private val label: String
    ) : Node<String, Unit, String> {
        private val _state = MutableStateFlow("")
        override val state = _state.asStateFlow()
        private val _outputs = MutableSharedFlow<String>(extraBufferCapacity = 1)
        override val outputs = _outputs.asSharedFlow()

        override fun onEvent(event: Unit) = Unit

        suspend fun emitOutput(value: String) {
            _outputs.emit(value)
        }
    }

    private class LifecycleAwareTestNode(
        private val label: String
    ) : Node<Unit, Unit, Unit>, LifecycleAwareNode {
        override val state = MutableStateFlow(Unit)
        override val outputs = MutableSharedFlow<Unit>()
        override fun onEvent(event: Unit) = Unit

        var attachCount = 0
        var detachCount = 0

        override fun onAttach() {
            attachCount++
        }

        override fun onDetach() {
            detachCount++
        }
    }

    private class TrackingNode(
        private val label: String
    ) : Node<Unit, Unit, Unit>, LifecycleAwareNode {
        override val state = MutableStateFlow(Unit)
        override val outputs = MutableSharedFlow<Unit>()
        override fun onEvent(event: Unit) = Unit

        var attachCount = 0
        var detachCount = 0

        override fun onAttach() {
            attachCount++
        }

        override fun onDetach() {
            detachCount++
        }
    }
}

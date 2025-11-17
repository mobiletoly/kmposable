package dev.goquick.kmposable.compose.viewmodel

import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertTrue

class KmposableViewModelTest {

    @Test
    fun disposesRuntimeWhenViewModelClears() {
        val node = TestNode()
        val runtime = TestRuntime(node)
        val viewModel = TestViewModel(runtime)

        viewModel.triggerClear()

        assertTrue(node.detached)
    }

    private class TestViewModel(
        runtime: TestRuntime
    ) : NavFlowViewModel<TestOutput>(runtime) {
        fun triggerClear() {
            onCleared()
        }
    }

    private class TestRuntime(
        node: TestNode
    ) : NavFlow<TestOutput, DefaultStackEntry<TestOutput>>(
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        rootNode = node
    )

    private class TestNode : Node<Unit, Unit, TestOutput>, LifecycleAwareNode {
        private val _state = MutableStateFlow(Unit)
        override val state = _state.asStateFlow()
        private val _outputs = MutableSharedFlow<TestOutput>(extraBufferCapacity = 1)
        override val outputs = _outputs.asSharedFlow()
        override fun onEvent(event: Unit) = Unit

        var detached = false

        override fun onDetach() {
            detached = true
        }
    }

    private sealed interface TestOutput
}

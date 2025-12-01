package dev.goquick.kmposable.compose

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.ResultOnlyNode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeRendererTest {

    @Test
    fun registersRendererForNodeType() {
        val renderer = NodeRendererBuilder<Unit>().apply {
            register<TestNode> { }
        }.build()

        assertTrue(renderer.canRender(TestNode()))
    }

    @Test
    fun registersRendererForResultOnlyNode() {
        val renderer = NodeRendererBuilder<Unit>().apply {
            registerResultOnly<ResultOnlyTestNode> { }
        }.build()

        @Suppress("UNCHECKED_CAST")
        val nodeAsOut = ResultOnlyTestNode() as Node<*, *, Unit>
        assertTrue(renderer.canRender(nodeAsOut))
    }

    @Test
    fun fallbackRendererHandlesUnknownNodes() {
        val renderer = NodeRendererBuilder<Unit>().apply {
            fallback { }
        }.build()

        assertTrue(renderer.canRender(TestNode()))
    }

    @Test
    fun missingRendererReported() {
        val renderer = NodeRendererBuilder<Unit>().build()
        assertFalse(renderer.canRender(TestNode()))
    }

    private class TestNode : Node<Unit, Unit, Unit> {
        override val state = MutableStateFlow(Unit)
        override fun onEvent(event: Unit) = Unit
        override val outputs = MutableSharedFlow<Unit>()
    }

    private class ResultOnlyTestNode : ResultOnlyNode<Unit, Unit, String>(
        parentScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        initialState = Unit
    ) {
        override fun onEvent(event: Unit) = Unit
        override val result = MutableSharedFlow<dev.goquick.kmposable.core.KmposableResult<String>>()
    }
}

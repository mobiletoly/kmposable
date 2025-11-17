package dev.goquick.kmposable.compose

import dev.goquick.kmposable.core.Node
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
}

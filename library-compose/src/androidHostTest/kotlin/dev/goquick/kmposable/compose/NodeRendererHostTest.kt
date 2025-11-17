package dev.goquick.kmposable.compose

import androidx.compose.runtime.remember
import dev.goquick.kmposable.core.Node
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeRendererHostTest {

    @Test
    fun dispatchesRegisteredRenderer() = runBlocking {
        withComposeHostTest {
            var renderedLabel: String? = null
            setContent {
                val renderer = remember {
                    nodeRenderer<RendererTestOutput> {
                        register<RendererTestLabelNode> { node -> renderedLabel = node.label }
                    }
                }

                renderer.Render(RendererTestLabelNode("Hello"))
            }

            awaitIdle()
            assertEquals("Hello", renderedLabel)
        }
    }

    @Test
    fun fallsBackWhenNoRendererRegistered() = runBlocking {
        withComposeHostTest {
            var fallbackCount = 0
            setContent {
                val renderer = remember {
                    nodeRenderer<RendererTestOutput> {
                        fallback { fallbackCount++ }
                    }
                }

                assertTrue(renderer.canRender(RendererTestLabelNode("unused")))
                renderer.Render(RendererTestLabelNode("unused"))
            }

            awaitIdle()
            assertTrue(fallbackCount >= 1)
        }
    }
}

private sealed interface RendererTestOutput

private class RendererTestLabelNode(
    val label: String
) : Node<String, Unit, RendererTestOutput> {
    private val _state = MutableStateFlow(label)
    override val state = _state.asStateFlow()
    private val _outputs = MutableSharedFlow<RendererTestOutput>(extraBufferCapacity = 1)
    override val outputs = _outputs.asSharedFlow()
    override fun onEvent(event: Unit) = Unit
}

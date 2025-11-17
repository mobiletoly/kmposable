package dev.goquick.kmposable.compose

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.compose.nodeRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals

class KmposableHostAndroidHostTest {

    @Test
    fun reRendersWhenNavigatorStackChanges() = runBlocking {
        val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val runtime = TestRuntime(runtimeScope)
        runtime.start()
        try {
            withComposeHostTest {
                var lastRendered: String? = null
                setContent {
                    val renderer = remember {
                        nodeRenderer<HostTestOutput> {
                            register<HostTestNode> { node ->
                                SideEffect {
                                    lastRendered = node.label
                                }
                            }
                        }
                    }
                    NavFlowHost(navFlow = runtime, renderer = renderer, enableBackHandler = false)
                }

                awaitIdle()
                assertEquals("Root", lastRendered)

                runtime.push(HostTestNode("Details"))
                awaitIdle()
                awaitIdle()
                assertEquals("Details", lastRendered)

                runtime.pop()
                awaitIdle()
                awaitIdle()
                assertEquals("Root", lastRendered)
            }
        } finally {
            runtimeScope.cancel()
        }
    }

    private class TestRuntime(scope: CoroutineScope) :
        NavFlow<HostTestOutput, DefaultStackEntry<HostTestOutput>>(
            appScope = scope,
            rootNode = HostTestNode("Root")
        )
}

private sealed interface HostTestOutput

private class HostTestNode(
    val label: String
) : Node<String, Unit, HostTestOutput> {
    private val _state = MutableStateFlow(label)
    override val state = _state.asStateFlow()
    private val _outputs = MutableSharedFlow<HostTestOutput>(extraBufferCapacity = 1)
    override val outputs = _outputs.asSharedFlow()
    override fun onEvent(event: Unit) = Unit
}

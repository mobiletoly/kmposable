package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.StatefulNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NavFlowScriptTest {

    @Test
    fun script_can_drive_navigation_and_await_outputs() = runBlocking {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val navFlow = NavFlow(
            appScope = appScope,
            rootNode = EmittingNode("root", appScope)
        )
        navFlow.start()

        val job = navFlow.launchNavFlowScript(this) {
            val first = awaitOutputOfType<TestOutput.Message>()
            assertEquals("root", first.payload)

            showRoot { EmittingNode("first", appScope) }

            val second = awaitOutputOfType<TestOutput.Message>()
            assertEquals("first", second.payload)

            showRoot { EmittingNode("second", appScope) }

            val third = awaitOutput<TestOutput.Message> {
                it is TestOutput.Message && it.payload == "second"
            }
            assertEquals("second", third.payload)
        }

        val emitter = launch {
            repeat(3) {
                delay(10)
                navFlow.sendEvent(TestEvent.Emit)
            }
        }

        job.join()
        emitter.cancel()
        navFlow.dispose()
        appScope.cancel()

        val top = navFlow.navState.value.top as EmittingNode
        assertEquals("second", top.state.value)
    }

    private class EmittingNode(
        private val label: String,
        parentScope: CoroutineScope
    ) : StatefulNode<String, TestEvent, TestOutput>(
        parentScope = parentScope,
        initialState = label,
        id = label
    ) {
        override fun onEvent(event: TestEvent) {
            scope.launch {
                when (event) {
                    TestEvent.Emit -> emitOutput(TestOutput.Message(label))
                    TestEvent.EmitSpecial -> emitOutput(TestOutput.Special)
                }
            }
        }
    }

    private sealed interface TestOutput {
        data class Message(val payload: String) : TestOutput
        data object Special : TestOutput
    }

    private sealed interface TestEvent {
        data object Emit : TestEvent
        data object EmitSpecial : TestEvent
    }

    @Test
    fun awaitOutputCase_maps_outputs_without_manual_when() = runBlocking {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val navFlow = NavFlow(
            appScope = appScope,
            rootNode = EmittingNode("root", appScope)
        )
        navFlow.start()

        val job = navFlow.launchNavFlowScript(this) {
            val value: String = awaitOutputCase {
                on<TestOutput.Special> { "special" }
                on<TestOutput.Message> { it.payload }
            }
            assertEquals("special", value)
        }

        launch {
            navFlow.sendEvent(TestEvent.EmitSpecial)
        }

        job.join()
        navFlow.dispose()
        appScope.cancel()
    }

    @Test
    fun pushForResult_returns_expected_output() = runBlocking {
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val navFlow = NavFlow(
            appScope = appScope,
            rootNode = EmittingNode("root", appScope)
        )
        navFlow.start()

        val job = navFlow.launchNavFlowScript(this) {
            val result = pushForResult(
                factory = { EmittingNode("child", appScope) },
                mapper = { output -> if (output is TestOutput.Special) output else null }
            )
            assertEquals(TestOutput.Special, result)
        }

        launch {
            delay(10)
            navFlow.sendEvent(TestEvent.EmitSpecial)
        }

        job.join()
        navFlow.dispose()
        appScope.cancel()
    }
}

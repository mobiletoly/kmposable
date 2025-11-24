package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import dev.goquick.kmposable.runtime.awaitOutputCase
import dev.goquick.kmposable.runtime.pushForResult
import dev.goquick.kmposable.runtime.runScript
import dev.goquick.kmposable.runtime.updateTopNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

class FlowScriptHelpersTest {

    @Test
    fun runScriptAlias_updates_top_node_via_helper() = runBlocking {
        val navFlow = NavFlow(
            appScope = this,
            rootNode = MutableNode(this)
        )
        navFlow.start()

        var traced = false
        val job = navFlow.runScript(this, onTrace = { traced = true }) {
            trace { "updating" }
            updateTopNode<TestOutput, MutableNode> {
                set("updated")
            }
        }

        withTimeout(2_000) { job.join() }
        val top = navFlow.currentTopNode() as MutableNode
        assertEquals("updated", top.state.value)
        assertTrue(traced)
        navFlow.dispose()
    }

    @Test
    fun awaitOutputCase_and_pushForResult_map_outputs_and_pop_stack() = runBlocking {
        val factory = SimpleNavFlowFactory<TestOutput> {
            NavFlow(
                appScope = this,
                rootNode = RootNode(this)
            )
        }
        val scenario = factory.createTestScenario(this).start()

        var capturedAction: String? = null
        var capturedResult: Int? = null

        val job = scenario.launchScript {
            capturedAction = awaitOutputCase {
                on<TestOutput.Action> { it.name }
            }
            capturedResult = pushForResult(
                factory = { ResultEmitterNode(this@runBlocking, value = 7) },
                mapper = { output -> (output as? TestOutput.Result)?.value }
            )
        }

        try {
            scenario.send(RootEvent.EmitAction("hello"))
            withTimeout(2_000) { job.join() }

            scenario.assertStackSize(1)
            assertEquals("hello", capturedAction)
            assertEquals(7, capturedResult)
        } finally {
            scenario.finish()
        }
    }

    private class MutableNode(parentScope: CoroutineScope) :
        StatefulNode<String, Unit, TestOutput>(parentScope, initialState = "initial") {
        override fun onEvent(event: Unit) = Unit
        fun set(value: String) = updateState { value }
    }

    private class RootNode(parentScope: CoroutineScope) :
        StatefulNode<Unit, RootEvent, TestOutput>(parentScope, initialState = Unit, id = "root") {
        override fun onEvent(event: RootEvent) {
            when (event) {
                is RootEvent.EmitAction -> tryEmitOutput(TestOutput.Action(event.name))
            }
        }
    }

    private class ResultEmitterNode(
        parentScope: CoroutineScope,
        private val value: Int
    ) : StatefulNode<Unit, Unit, TestOutput>(parentScope, initialState = Unit, id = "result") {
        override fun onAttach() {
            scope.launch {
                // Let NavFlow start observing outputs before emitting.
                yield()
                emitOutput(TestOutput.Result(value))
            }
        }

        override fun onEvent(event: Unit) = Unit
    }

    private sealed interface RootEvent {
        data class EmitAction(val name: String) : RootEvent
    }

    private sealed interface TestOutput {
        data class Action(val name: String) : TestOutput
        data class Result(val value: Int) : TestOutput
    }
}

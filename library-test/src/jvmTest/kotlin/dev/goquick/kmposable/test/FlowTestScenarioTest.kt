package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTestScenarioTest {

    @Test
    fun scenario_runs_flow_without_ui() = runTest {
        val factory = SimpleNavFlowFactory<TestOutput> {
            NavFlow(
                appScope = this,
                rootNode = RootNode(this)
            )
        }

        factory.createTestScenario(this)
            .start()
            .assertTopNodeTag("Root")
            .send(TestEvent.Emit("next"))
            .assertNextOutput(TestOutput("next"))
            .finish()
    }

    private class RootNode(
        parentScope: CoroutineScope
    ) : StatefulNode<Unit, TestEvent, TestOutput>(
        parentScope = parentScope,
        initialState = Unit,
        id = "Root"
    ) {
        override fun onEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Emit -> {
                    tryEmitOutput(TestOutput(event.target))
                }
            }
        }
    }

    private sealed interface TestEvent {
        data class Emit(val target: String) : TestEvent
    }

    private data class TestOutput(val target: String)
}

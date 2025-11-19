package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import dev.goquick.kmposable.runtime.awaitOutputOfType
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTestScenarioScriptTest {

    @Test
    fun launchScript_runs_same_logic_as_production() = runTest {
        val factory = SimpleNavFlowFactory<TestOutput> {
            NavFlow(
                appScope = this,
                rootNode = EmittingNode("root", this)
            )
        }
        val scenario = factory.createTestScenario(this).start()

        val job = scenario.launchScript {
            val result = awaitOutputOfType<TestOutput.Message>()
            check(result.payload == "root")
        }

        job.join()
        assertTrue(job.isCompleted)
        scenario.finish()
    }

    @Test
    fun awaitTopNodeIs_waits_for_script_navigation() = runTest {
        val factory = SimpleNavFlowFactory<TestOutput> {
            NavFlow(
                appScope = this,
                rootNode = FirstNode(this)
            )
        }
        val scenario = factory.createTestScenario(this).start()

        val job = scenario.launchScript {
            showRoot { SecondNode(this@runTest) }
        }

        scenario.awaitTopNodeIs<SecondNode>()
        job.cancelAndJoin()
        scenario.finish()
    }

    @Test
    fun awaitStackHelpers_observe_push_sequences() = runTest {
        val factory = SimpleNavFlowFactory<TestOutput> {
            NavFlow(
                appScope = this,
                rootNode = FirstNode(this)
            )
        }
        val scenario = factory.createTestScenario(this).start()

        val job = scenario.launchScript {
            pushNode { SecondNode(this@runTest) }
            pushNode { ThirdNode(this@runTest) }
        }

        scenario.awaitStackSize(3)
            .awaitStackTags("first", "second", "third")

        job.cancelAndJoin()
        scenario.finish()
    }

    private class EmittingNode(
        private val label: String,
        parentScope: CoroutineScope
    ) : StatefulNode<String, Unit, TestOutput>(
        parentScope = parentScope,
        initialState = label,
        id = label
    ) {
        override fun onAttach() {
            scope.launch {
                emitOutput(TestOutput.Message(label))
            }
        }

        override fun onEvent(event: Unit) = Unit
    }

    private sealed interface TestOutput {
        data class Message(val payload: String) : TestOutput
    }

    private open class LabelNode(
        label: String,
        parentScope: CoroutineScope
    ) : StatefulNode<String, Unit, TestOutput>(
        parentScope = parentScope,
        initialState = label,
        id = label
    ) {
        override fun onEvent(event: Unit) = Unit
    }

    private class FirstNode(parentScope: CoroutineScope) : LabelNode("first", parentScope)
    private class SecondNode(parentScope: CoroutineScope) : LabelNode("second", parentScope)
    private class ThirdNode(parentScope: CoroutineScope) : LabelNode("third", parentScope)
}

package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultOnlyNode
import dev.goquick.kmposable.core.ResultfulStatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FlowTestScenarioPushResultTest {

    @Test
    fun pushResultNode_returns_ok_and_auto_pops() = runTest {
        val navFlow: NavFlow<Unit, *> = SimpleNavFlow(rootNode = NoOpNode(this))
        val scenario = FlowTestScenario(navFlow, this).start()

        val result = scenario.pushResultNode(autoPop = true) {
            DummyResultNode(parentScope = this, result = "hello")
        }

        assertTrue(result is KmposableResult.Ok && result.value == "hello")
        scenario.finish()
    }

    @Test
    fun pushResultNode_can_return_canceled_on_pop() = runTest {
        val navFlow: NavFlow<Unit, *> = SimpleNavFlow(rootNode = NoOpNode(this))
        val scenario = FlowTestScenario(navFlow, this).start()

        val resultDeferred = async {
            scenario.pushResultNode(autoPop = false) {
                DummyResultNode(parentScope = this, result = "value")
            }
        }
        // pop manually to cancel
        navFlow.pop()
        val result = resultDeferred.await()
        assertTrue(result is KmposableResult.Canceled)
        scenario.finish()
    }
}

private class NoOpNode(scope: kotlinx.coroutines.CoroutineScope) :
    ResultfulStatefulNode<Unit, Nothing, Unit, Unit>(
        parentScope = scope,
        initialState = Unit
    ) {
    override fun onEvent(event: Nothing) = Unit
}

private class DummyResultNode(
    parentScope: kotlinx.coroutines.CoroutineScope,
    private val result: String
) : ResultOnlyNode<Unit, Nothing, String>(
    parentScope = parentScope,
    initialState = Unit
) {
    override fun onEvent(event: Nothing) = Unit
    override fun onAttach() {
        super.onAttach()
        scope.launch {
            emitOk(result)
        }
    }
}

package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultOnlyNode
import dev.goquick.kmposable.core.ResultfulStatefulNode
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTestScenarioPushResultTest {

    @Test
    fun pushResultNode_returns_ok_and_auto_pops() = runTest {
        val navFlow: NavFlow<Unit, DefaultStackEntry<Unit>> =
            NavFlow(appScope = this, rootNode = NoOpNode(this))
        val scenario = FlowTestScenario(navFlow, this).start()

        val result = scenario.pushResultNode(autoPop = true) {
            DummyResultNode(parentScope = this, resultValue = "hello")
        }

        assertTrue(result is KmposableResult.Ok && result.value == "hello")
        scenario.finish()
        advanceUntilIdle()
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
    private val resultValue: String
) : ResultOnlyNode<Unit, Nothing, String>(
    parentScope = parentScope,
    initialState = Unit
    ) {
        override fun onEvent(event: Nothing) = Unit
        override fun onAttach() {
            super.onAttach()
            scope.launch {
                delay(50)
                emitOk(resultValue)
        }
    }

    override fun onDetach() {
        tryEmitCanceled()
        super.onDetach()
    }
}

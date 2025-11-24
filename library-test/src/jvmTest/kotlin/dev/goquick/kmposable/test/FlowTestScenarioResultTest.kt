package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTestScenarioResultTest {

    @Test
    fun awaitTopResultReturnsOk() = runTest {
        val factory = SimpleNavFlowFactory<ResultOutput> {
            NavFlow(
                appScope = this,
                rootNode = ResultNodeStub(this)
            )
        }

        val scenario = factory.createTestScenario(this).start()
        advanceUntilIdle()

        // Emit result
        val top = scenario.navFlow.currentTopNode() as ResultNodeStub
        top.emit(KmposableResult.Ok(ResultOutput("done")))

        val result = scenario.awaitTopResult<ResultOutput>()
        assertIs<KmposableResult.Ok<ResultOutput>>(result)
        assertEquals("done", result.value.payload)

        scenario.finish()
    }

    @Test
    fun awaitTopResultReturnsCanceledWhenNodeRemoved() = runTest {
        val factory = SimpleNavFlowFactory<ResultOutput> {
            NavFlow(
                appScope = this,
                rootNode = ResultNodeStub(this)
            )
        }

        val scenario = factory.createTestScenario(this).start()
        advanceUntilIdle()

        scenario.pop()
        val result = scenario.awaitTopResult<ResultOutput>()
        assertEquals(KmposableResult.Canceled, result)
        scenario.finish()
    }

    private data class ResultOutput(val payload: String)

    private class ResultNodeStub(
        parentScope: kotlinx.coroutines.CoroutineScope
    ) : StatefulNode<Unit, Unit, ResultOutput>(parentScope, Unit), ResultNode<ResultOutput> {
        private val resultFlow = MutableSharedFlow<KmposableResult<ResultOutput>>(extraBufferCapacity = 1)
        override val result = resultFlow
        override fun onEvent(event: Unit) = Unit
        suspend fun emit(result: KmposableResult<ResultOutput>) {
            resultFlow.emit(result)
        }
    }
}

package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

class FlowTestScenarioResultTest {

    @Test
    fun awaitTopResultReturnsOk() = runBlocking {
        val factory = SimpleNavFlowFactory<ResultOutput> {
            NavFlow(
                appScope = this,
                rootNode = ResultNodeStub(this)
            )
        }

        val scenario = factory.createTestScenario(this).start()
        try {
            val awaiting = async { scenario.awaitTopResult<ResultOutput>() }

            yield() // let awaitTopResult start collecting
            val top = scenario.navFlow.currentTopNode() as ResultNodeStub
            top.emit(KmposableResult.Ok(ResultOutput("done")))

            val result = withTimeout(2_000) { awaiting.await() }
            assertIs<KmposableResult.Ok<ResultOutput>>(result)
            assertEquals("done", result.value.payload)
        } finally {
            scenario.finish()
        }
    }

    @Test
    fun awaitTopResultReturnsCanceledWhenNodeRemoved() = runBlocking {
        val factory = SimpleNavFlowFactory<ResultOutput> {
            NavFlow(
                appScope = this,
                rootNode = ResultNodeStub(this)
            )
        }

        val scenario = factory.createTestScenario(this).start()
        try {
            val awaiting = async { scenario.awaitTopResult<ResultOutput>() }

            yield() // let awaitTopResult start collecting
            scenario.navFlow.replaceTop(NonResultNode(this))
            val result = withTimeout(2_000) { awaiting.await() }
            assertEquals(KmposableResult.Canceled, result)
        } finally {
            scenario.finish()
        }
    }

    private data class ResultOutput(val payload: String)

    private class ResultNodeStub(
        parentScope: kotlinx.coroutines.CoroutineScope
    ) : StatefulNode<Unit, Unit, ResultOutput>(parentScope, Unit), ResultNode<ResultOutput> {
        private val resultFlow = MutableSharedFlow<KmposableResult<ResultOutput>>(replay = 1)
        override val result = resultFlow
        override fun onEvent(event: Unit) = Unit
        suspend fun emit(result: KmposableResult<ResultOutput>) {
            resultFlow.emit(result)
        }
    }

    private class NonResultNode(parentScope: kotlinx.coroutines.CoroutineScope) :
        StatefulNode<Unit, Unit, ResultOutput>(parentScope, Unit) {
        override fun onEvent(event: Unit) = Unit
    }
}

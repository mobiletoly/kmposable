package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultfulStatefulNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class ResultfulStatefulNodeTest {

    @Test
    fun emits_ok_result() = runBlocking(Dispatchers.Unconfined) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val node = ResultfulStub(scope)

            // Use tryEmit instead of suspend emit to avoid deadlock
            node.tryEmitValue("hi")

            val result = withTimeout(200) { node.result.first() }
            assertEquals(KmposableResult.Ok("hi"), result)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun emits_canceled_result() = runBlocking(Dispatchers.Unconfined) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val node = ResultfulStub(scope)

            // Use tryEmit instead of suspend emit to avoid deadlock
            node.tryEmitCancel()

            val result = withTimeout(200) { node.result.first() }
            assertEquals(KmposableResult.Canceled, result)
        } finally {
            scope.cancel()
        }
    }

    private class ResultfulStub(
        parentScope: kotlinx.coroutines.CoroutineScope
    ) : ResultfulStatefulNode<Unit, Unit, Unit, String>(
        parentScope = parentScope,
        initialState = Unit
    ) {
        suspend fun emitValue(value: String) = emitOk(value)
        fun tryEmitValue(value: String) = tryEmitOk(value)
        suspend fun emitCancel() = emitCanceled()
        fun tryEmitCancel() = tryEmitCanceled()
        override fun onEvent(event: Unit) = Unit
    }
}

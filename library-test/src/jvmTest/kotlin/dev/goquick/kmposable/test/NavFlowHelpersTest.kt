package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.pushAndAwaitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
class NavFlowHelpersTest {

    @Test
    fun pushAndAwaitResult_returns_ok_and_pops_by_default() = runBlocking {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()

        try {
            val child = ResultNodeStub(this, resultValue = "value")
            println("Test ok/pops: pushing child, stackSize=${navFlow.navState.value.size}")
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child)
            }
            println("Test ok/pops: got result=$result stackSize=${navFlow.navState.value.size}")
            assertEquals(KmposableResult.Ok("value"), result)
            assertTrue(navFlow.navState.value.size == 1) // popped back to root
        } finally {
            navFlow.dispose()
        }
    }


    @Test
    fun pushAndAwaitResult_returns_canceled_when_no_result() = runBlocking {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()

        try {
            val child = ResultNodeStub(this, emitResult = false)
            println("Test canceled: pushing child, stackSize=${navFlow.navState.value.size}")
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child)
            }
            println("Test canceled: got result=$result stackSize=${navFlow.navState.value.size}")
            assertEquals(KmposableResult.Canceled, result)
        } finally {
            navFlow.dispose()
        }
    }

    @Test
    fun pushAndAwaitResult_respects_autoPop_false() = runBlocking {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()

        try {
            val child = ResultNodeStub(this, resultValue = "keep")
            println("Test autopop=false: pushing child, stackSize=${navFlow.navState.value.size}")
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child, autoPop = false)
            }
            println("Test autopop=false: got result=$result stackSize=${navFlow.navState.value.size}")
            assertEquals(KmposableResult.Ok("keep"), result)
            assertEquals(2, navFlow.navState.value.size) // child still on stack
            println("pushAndAwaitResult_respects_autoPop_false - runBlocking is near the end")
        } finally {
            navFlow.dispose()
        }
    }

    @Test
    fun pushAndAwaitResult_does_not_pop_after_dispose() = runBlocking {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()

        val child = ResultNodeStub(this, resultValue = "late", emitImmediately = false)
        val deferred = async {
            navFlow.pushAndAwaitResult(child)
        }

        // Wait until child is on the stack, then dispose the runtime.
        withTimeout(200) {
            while (navFlow.navState.value.size < 2) {
                yield()
            }
        }
        navFlow.dispose()

        // Emit the result after dispose; helper must not crash trying to pop.
        child.emitResultNow()
        val result = withTimeout(200) { deferred.await() }
        assertEquals(KmposableResult.Ok("late"), result)
        assertTrue(!navFlow.isStarted())
    }

    private class ResultNodeStub(
        parentScope: kotlinx.coroutines.CoroutineScope,
        private val emitResult: Boolean = true,
        private val resultValue: String = "ok",
        private val emitImmediately: Boolean = true
    ) : StatefulNode<Unit, Unit, Unit>(parentScope, Unit), ResultNode<String> {
        private val flow =
            MutableSharedFlow<KmposableResult<String>>(replay = 1, extraBufferCapacity = 1)
        override val result = flow

        init {
            println("ResultNodeStub init emitResult=$emitResult value=$resultValue")
            if (emitImmediately) {
                emitResultNow()
            }
        }

        fun emitResultNow(result: KmposableResult<String> = defaultResult()) {
            flow.tryEmit(result)
        }

        private fun defaultResult(): KmposableResult<String> {
            return if (emitResult) {
                KmposableResult.Ok(resultValue)
            } else {
                KmposableResult.Canceled
            }
        }

        override fun onAttach() = Unit

        override fun onEvent(event: Unit) = Unit
    }
}

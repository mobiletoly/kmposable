package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.launchPushAndAwaitResult
import dev.goquick.kmposable.runtime.pushAndAwaitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
class NavFlowHelpersTest {

    @Test
    fun pushAndAwaitResult_returns_ok_and_pops_by_default() = runTest {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()
        advanceUntilIdle()

        try {
            val child = ResultNodeStub(this, resultValue = "value")
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child)
            }
            advanceUntilIdle()
            assertEquals(KmposableResult.Ok("value"), result)
            assertTrue(navFlow.navState.value.size == 1) // popped back to root
        } finally {
            navFlow.dispose()
        }
    }


    @Test
    fun pushAndAwaitResult_returns_canceled_when_no_result() = runTest {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()
        advanceUntilIdle()

        try {
            val child = ResultNodeStub(this, emitResult = false)
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child)
            }
            advanceUntilIdle()
            assertEquals(KmposableResult.Canceled, result)
        } finally {
            navFlow.dispose()
        }
    }

    @Test
    fun pushAndAwaitResult_respects_autoPop_false() = runTest {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()
        advanceUntilIdle()

        try {
            val child = ResultNodeStub(this, resultValue = "keep")
            val result = withTimeout(200) {
                navFlow.pushAndAwaitResult(child, autoPop = false)
            }
            advanceUntilIdle()
            assertEquals(KmposableResult.Ok("keep"), result)
            assertEquals(2, navFlow.navState.value.size) // child still on stack
        } finally {
            navFlow.dispose()
        }
    }

    @Test
    fun pushAndAwaitResult_does_not_pop_after_dispose() = runTest {
        val node = ResultNodeStub(this)
        val navFlow = NavFlow(
            appScope = this,
            rootNode = node
        )
        navFlow.start()
        advanceUntilIdle()

        val child = ResultNodeStub(this, resultValue = "late", emitImmediately = false)
        val deferred = async {
            navFlow.pushAndAwaitResult(child)
        }
        advanceUntilIdle()

        // Wait until child is on the stack, then dispose the runtime.
        withTimeout(200) {
            while (navFlow.navState.value.size < 2) {
                yield()
            }
        }
        advanceUntilIdle()
        navFlow.dispose()
        advanceUntilIdle()

        // Emit the result after dispose; helper must not crash trying to pop.
        child.emitResultNow()
        advanceUntilIdle()
        val result = withTimeout(200) { deferred.await() }
        assertEquals(KmposableResult.Ok("late"), result)
        assertTrue(!navFlow.isStarted())
    }

    @Test
    fun pushAndAwaitResult_factory_overload_invokes_callback_once() = runTest {
        val root = ResultNodeStub(this)
        val navFlow = NavFlow(appScope = this, rootNode = root).also { it.start() }
        advanceUntilIdle()

        var factoryCalls = 0
        var callbackResult: KmposableResult<String>? = null
        val result = navFlow.pushAndAwaitResult(
            factory = {
                factoryCalls++
                ResultNodeStub(this, resultValue = "factory")
            },
            onResult = { callbackResult = it }
        )
        advanceUntilIdle()

        assertEquals(1, factoryCalls)
        assertEquals(KmposableResult.Ok("factory"), result)
        assertEquals(result, callbackResult)
        assertTrue(navFlow.navState.value.size == 1)
        navFlow.dispose()
    }

    @Test
    fun pushAndAwaitResult_tryPushAndPopIfStarted_guard_unstarted() = runTest {
        val root = ResultNodeStub(this)
        val navFlow = NavFlow(appScope = this, rootNode = root)
        advanceUntilIdle()

        // Test pushIfStarted when not started - create a separate scope for the temp node
        // so it doesn't block the test scope
        val tempScope = CoroutineScope(SupervisorJob())
        try {
            val tempNode = ResultNodeStub(tempScope, emitImmediately = false)
            val pushed = navFlow.pushIfStarted(tempNode)
            advanceUntilIdle()
            assertFalse(pushed) // not started yet
            assertFalse(navFlow.popIfStarted()) // safe no-op
            advanceUntilIdle()
        } finally {
            tempScope.cancel()
        }

        navFlow.start()
        advanceUntilIdle()
        val child = ResultNodeStub(this, resultValue = "x")
        assertTrue(navFlow.pushIfStarted(child))
        advanceUntilIdle()
        assertEquals(2, navFlow.navState.value.size)
        assertTrue(navFlow.popIfStarted())
        advanceUntilIdle()
        assertEquals(1, navFlow.navState.value.size)
        navFlow.dispose()
    }

    @Test
    fun launchPushAndAwaitResult_runs_in_scope_and_pops() = runTest {
        val root = ResultNodeStub(this)
        val navFlow = NavFlow(appScope = this, rootNode = root).also { it.start() }
        advanceUntilIdle()

        var callback: KmposableResult<String>? = null
        val job = launchPushAndAwaitResult(
            navFlow = navFlow,
            factory = { ResultNodeStub(this, resultValue = "from-launch") },
            onResult = { callback = it }
        )
        advanceUntilIdle()
        withTimeout(200) { job.join() }
        advanceUntilIdle()

        assertEquals(KmposableResult.Ok("from-launch"), callback)
        assertEquals(1, navFlow.navState.value.size) // autoPop=true by default
        navFlow.dispose()
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

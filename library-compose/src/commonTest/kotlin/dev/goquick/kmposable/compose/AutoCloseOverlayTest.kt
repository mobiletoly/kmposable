package dev.goquick.kmposable.compose

import dev.goquick.kmposable.core.AutoCloseOverlay
import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultfulStatefulNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import dev.goquick.kmposable.runtime.NavFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCloseOverlayTest {

    @Test
    fun pops_overlay_when_result_emitted() = runTest {
        val navFlow = navFlowWithRoot()
        val overlay = AutoClosingOverlayNode(backgroundScope)

        navFlow.push(overlay)
        val job = launch { runAutoCloseOverlay(navFlow, overlay) }

        overlay.emit(KmposableResult.Ok(Unit))
        job.join()

        assertEquals(1, navFlow.navState.value.size)
    }

    @Test
    fun respects_guard_when_shouldAutoClose_returns_false() = runTest {
        val navFlow = navFlowWithRoot()
        val overlay = GuardedOverlayNode(backgroundScope)

        navFlow.push(overlay)
        val job = launch { runAutoCloseOverlay(navFlow, overlay) }

        overlay.emit(KmposableResult.Ok(Unit))
        advanceUntilIdle()

        assertEquals(2, navFlow.navState.value.size)
        job.cancel()
    }

    private fun TestScope.navFlowWithRoot(): NavFlow<Unit, DefaultStackEntry<Unit>> {
        val navFlow = NavFlow<Unit, DefaultStackEntry<Unit>>(
            appScope = backgroundScope,
            rootNode = RootNode(backgroundScope)
        )
        navFlow.start()
        return navFlow
    }

    private class RootNode(scope: CoroutineScope) :
        StatefulNode<Unit, Unit, Unit>(scope, Unit) {
        override fun onEvent(event: Unit) = Unit
    }

    private open class AutoClosingOverlayNode(
        private val parentScope: CoroutineScope
    ) : ResultfulStatefulNode<Unit, Unit, Unit, Unit>(
        parentScope = parentScope,
        initialState = Unit
    ), AutoCloseOverlay<Unit>, PresentationAware {
        override val presentation: Presentation = Presentation.Overlay

        fun emit(result: KmposableResult<Unit>) {
            parentScope.launch { emitResult(result) }
        }

        override fun onEvent(event: Unit) = Unit
    }

    private class GuardedOverlayNode(scope: CoroutineScope) : AutoClosingOverlayNode(scope) {
        override fun shouldAutoClose(result: KmposableResult<Unit>): Boolean = false
    }
}

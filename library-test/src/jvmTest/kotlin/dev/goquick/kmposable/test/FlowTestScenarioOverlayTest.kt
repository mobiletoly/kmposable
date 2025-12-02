package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultOnlyNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import dev.goquick.kmposable.runtime.NavFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class FlowTestScenarioOverlayTest {

    @Test
    fun pushOverlayResult_returns_result_and_pops_by_default() = runTest {
        val navFlow = NavFlow<Unit, DefaultStackEntry<Unit>>(
            appScope = this,
            rootNode = BaseNode(this)
        )
        val scenario = FlowTestScenario(navFlow, this)
        scenario.start()

        val result = scenario.pushOverlayResult(factory = { OverlayDialogNode(this) })

        assertEquals(KmposableResult.Ok("overlay"), result)
        assertEquals(1, navFlow.navState.value.size)
    }

    private class BaseNode(scope: kotlinx.coroutines.CoroutineScope) :
        StatefulNode<Unit, Unit, Unit>(scope, Unit) {
        override fun onEvent(event: Unit) = Unit
    }

    private class OverlayDialogNode(scope: kotlinx.coroutines.CoroutineScope) :
        ResultOnlyNode<Unit, Unit, String>(scope, Unit),
        PresentationAware {
        override val presentation: Presentation = Presentation.Overlay

        init {
            scope.launch { emitOk("overlay") }
        }

        override fun onEvent(event: Unit) = Unit
    }
}

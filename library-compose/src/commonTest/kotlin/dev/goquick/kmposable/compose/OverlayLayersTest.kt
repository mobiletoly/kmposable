package dev.goquick.kmposable.compose

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayLayersTest {

    @Test
    fun splits_base_and_overlays_when_overlays_present() {
        val base = entry(BaseNode())
        val overlay1 = entry(OverlayNode())
        val overlay2 = entry(OverlayNode())
        val state = KmposableNavState(stack = listOf(base, overlay1, overlay2))

        val layers = state.toOverlayLayers()

        assertEquals(base, layers.base)
        assertEquals(listOf(overlay1, overlay2), layers.overlays)
    }

    @Test
    fun falls_back_to_top_when_all_overlays() {
        val overlay = entry(OverlayNode())
        val state = KmposableNavState(stack = listOf(overlay))

        val layers = state.toOverlayLayers()

        assertEquals(overlay, layers.base)
        assertTrue(layers.overlays.isEmpty())
    }

    private fun <OUT : Any> entry(node: Node<*, *, OUT>) = DefaultStackEntry(node)

    private open class BaseNode : Node<Unit, Unit, Unit> {
        override val state = MutableStateFlow(Unit)
        override fun onEvent(event: Unit) = Unit
        override val outputs = MutableSharedFlow<Unit>()
    }

    private class OverlayNode : BaseNode(), PresentationAware {
        override val presentation: Presentation = Presentation.Overlay
    }
}

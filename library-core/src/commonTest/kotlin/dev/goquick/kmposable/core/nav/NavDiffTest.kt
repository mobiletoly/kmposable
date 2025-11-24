package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update

private class StubNode : Node<Unit, Unit, Unit> {
    private val _state = MutableStateFlow(Unit)
    override val state = _state
    override fun onEvent(event: Unit) {
        _state.update { it }
    }
    override val outputs = emptyFlow<Unit>()
}

private fun navState(vararg entries: KmposableStackEntry<Unit>): KmposableNavState<Unit, KmposableStackEntry<Unit>> =
    KmposableNavState(entries.toList())

private fun entry(tag: String): KmposableStackEntry<Unit> =
    DefaultStackEntry(node = StubNode(), tag = tag)

class NavDiffTest {

    @Test
    fun diffWithNullPreviousTreatsAllAsPushed() {
        val a = entry("A")
        val b = entry("B")
        val current = navState(a, b)

        val diff = diffNavState(previous = null, current = current)

        assertTrue(diff.popped.isEmpty())
        assertEquals(listOf(a, b), diff.pushed)
    }

    @Test
    fun detectsPush() {
        val a = entry("A")
        val b = entry("B")
        val c = entry("C")

        val diff = diffNavState(navState(a, b), navState(a, b, c))

        assertTrue(diff.popped.isEmpty())
        assertEquals(listOf(c), diff.pushed)
    }

    @Test
    fun detectsPop() {
        val a = entry("A")
        val b = entry("B")
        val c = entry("C")

        val diff = diffNavState(navState(a, b, c), navState(a, b))

        assertEquals(listOf(c), diff.popped)
        assertTrue(diff.pushed.isEmpty())
    }

    @Test
    fun detectsReplaceAll() {
        val a = entry("A")
        val b = entry("B")
        val c = entry("C")

        val diff = diffNavState(navState(a, b), navState(c))

        assertEquals(listOf(b, a), diff.popped)
        assertEquals(listOf(c), diff.pushed)
    }

    @Test
    fun detectsNoOp() {
        val a = entry("A")
        val b = entry("B")

        val diff = diffNavState(navState(a, b), navState(a, b))

        assertTrue(diff.isNoOp)
        assertTrue(diff.popped.isEmpty())
        assertTrue(diff.pushed.isEmpty())
    }
}

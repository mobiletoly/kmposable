package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class StackNavigatorTest {

    @Test
    fun initialStateContainsRootNode() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))

        assertEquals(1, navigator.state.value.stack.size)
        assertSame(root, navigator.state.value.top)
    }

    @Test
    fun pushAddsNodeToTop() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val details = testNode("details")

        navigator.push(DefaultStackEntry(details))

        assertEquals(listOf(root, details), navigator.state.value.stack.map { it.node })
    }

    @Test
    fun popCannotRemoveLastNode() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))

        navigator.pop()

        assertEquals(1, navigator.state.value.stack.size)
        assertSame(root, navigator.state.value.top)
    }

    @Test
    fun replaceAllKeepsOnlyProvidedNode() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val list = testNode("list")

        navigator.push(DefaultStackEntry(list))
        val newRoot = testNode("new-root")
        navigator.replaceAll(DefaultStackEntry(newRoot))

        assertEquals(listOf(newRoot), navigator.state.value.stack.map { it.node })
    }

    @Test
    fun popAllRemovesEverythingAboveRoot() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val detail = testNode("detail")
        val edit = testNode("edit")
        navigator.push(DefaultStackEntry(detail))
        navigator.push(DefaultStackEntry(edit))

        val removed = navigator.popAll()

        assertEquals(listOf(edit, detail), removed.map { it.node })
        assertEquals(listOf(root), navigator.state.value.stack.map { it.node })
    }

    @Test
    fun replaceTopSwapsOnlyLastEntry() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val detail = testNode("detail")
        navigator.push(DefaultStackEntry(detail))
        val edit = testNode("edit")

        val removed = navigator.replaceTop(DefaultStackEntry(edit))

        assertSame(detail, removed?.node)
        assertEquals(listOf(root, edit), navigator.state.value.stack.map { it.node })
    }

    @Test
    fun popToCanRemoveInclusive() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val list = testNode("list")
        val detail = testNode("detail")
        val listEntry = DefaultStackEntry(list)
        navigator.push(listEntry)
        val detailEntry = DefaultStackEntry(detail)
        navigator.push(detailEntry)

        val removedExclusive = navigator.popTo(listEntry, inclusive = false)
        assertEquals(listOf(detail), removedExclusive.map { it.node })
        assertEquals(listOf(root, list), navigator.state.value.stack.map { it.node })

        val removedInclusive = navigator.popTo(listEntry, inclusive = true)
        assertEquals(listOf(list), removedInclusive.map { it.node })
        assertEquals(listOf(root), navigator.state.value.stack.map { it.node })
    }

    private fun testNode(id: String) = object : Node<Unit, Unit, Nothing> {
        override val state = MutableStateFlow(Unit)
        override fun onEvent(event: Unit) = Unit
        override val outputs = MutableSharedFlow<Nothing>(extraBufferCapacity = 1)
    }
}

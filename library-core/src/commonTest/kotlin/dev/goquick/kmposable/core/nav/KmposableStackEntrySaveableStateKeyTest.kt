package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KmposableStackEntrySaveableStateKeyTest {

    @Test
    fun defaultStackEntryEqualityIncludesSaveableStateKey() {
        val node = testNode("shared")
        val first = DefaultStackEntry(node = node, tag = "shared", saveableStateKey = "first")
        val second = DefaultStackEntry(node = node, tag = "shared", saveableStateKey = "second")

        assertNotEquals(first, second)
        assertNotEquals(KmposableNavState(listOf(first)), KmposableNavState(listOf(second)))
    }

    @Test
    fun defaultSaveableStateKeyStaysStableWhenCustomEntryHashCodeChanges() {
        val entry = MutableCustomEntry(
            node = testNode("custom"),
            tag = "custom",
            revision = 1
        )

        val initialKey = entry.saveableStateKey
        entry.revision = 2
        entry.tag = "updated"

        assertEquals(initialKey, entry.saveableStateKey)
    }

    private data class MutableCustomEntry(
        override val node: Node<*, *, Unit>,
        override var tag: String?,
        var revision: Int
    ) : KmposableStackEntry<Unit>

    private fun testNode(id: String) = object : Node<String, Unit, Unit> {
        override val state = MutableStateFlow(id)
        override fun onEvent(event: Unit) = Unit
        override val outputs = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }
}

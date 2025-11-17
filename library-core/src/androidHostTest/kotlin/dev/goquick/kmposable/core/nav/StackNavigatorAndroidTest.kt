package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class StackNavigatorAndroidTest {

    @Test
    fun stackNavigatorBehavesConsistentlyOnAndroid() {
        val root = testNode("root")
        val navigator = KmposableStackNavigator(DefaultStackEntry(root))
        val next = testNode("next")

        navigator.push(DefaultStackEntry(next))
        navigator.replaceAll(DefaultStackEntry(testNode("replacement")))

        assertEquals(1, navigator.state.value.stack.size)
    }

    private fun testNode(id: String) = object : Node<Unit, Unit, Nothing> {
        override val state = MutableStateFlow(Unit)
        override fun onEvent(event: Unit) = Unit
        override val outputs = MutableSharedFlow<Nothing>(extraBufferCapacity = 1)
    }
}

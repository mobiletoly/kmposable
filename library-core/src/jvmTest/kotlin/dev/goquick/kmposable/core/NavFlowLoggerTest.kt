package dev.goquick.kmposable.core

import dev.goquick.kmposable.core.logging.NavFlowLogger
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class NavFlowLoggerTest {

    @Test
    fun logger_receives_attach_output_detach_events() = runTest {
        val events = mutableListOf<String>()
        val logger = object : NavFlowLogger<String> {
            override fun onAttach(node: Node<*, *, String>) {
                events += "attach:${(node as? Taggable)?.tag}"
            }

            override fun onOutput(node: Node<*, *, String>, output: String) {
                events += "output:$output"
            }

            override fun onDetach(node: Node<*, *, String>) {
                events += "detach:${(node as? Taggable)?.tag}"
            }
        }
        val root = LoggingNode(parentScope = this, tag = "root")
        val navFlow = NavFlow<String, DefaultStackEntry<String>>(
            appScope = this,
            rootNode = root,
            logger = logger
        )
        navFlow.start()

        val child = LoggingNode(parentScope = this, tag = "child")
        navFlow.push(child)
        child.emit("hello")
        advanceUntilIdle()
        navFlow.pop()
        navFlow.dispose()
        advanceUntilIdle()

        assertEquals(
            listOf("attach:root", "attach:child", "output:hello", "detach:child", "detach:root"),
            events
        )
    }

    private interface Taggable {
        val tag: String
    }

    private class LoggingNode(
        parentScope: kotlinx.coroutines.CoroutineScope,
        override val tag: String
    ) : StatefulNode<Unit, Unit, String>(parentScope, Unit), Taggable {
        override fun onEvent(event: Unit) = Unit
        fun emit(value: String) {
            scope.launch { emitOutput(value) }
        }
    }
}

package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultfulStatefulNode
import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class NavFlowHelpersTest {

    @Test
    fun returnsCanceledWhenAwaitedNodeIsDismissedExternally() = runTest {
        val navFlow = navFlowWithRoot()
        val awaitingNode = ResultTestNode(backgroundScope, "awaiting")
        val replacement = PassiveNode(backgroundScope, "replacement")

        val deferred = async { navFlow.pushAndAwaitResult(awaitingNode) }
        advanceUntilIdle()

        navFlow.pop()
        navFlow.push(replacement)

        assertEquals(KmposableResult.Canceled, deferred.await())
        assertSame(replacement, navFlow.currentTopNode())
    }

    @Test
    fun autoPopRemovesAwaitedNodeAfterResult() = runTest {
        val navFlow = navFlowWithRoot()
        val awaitingNode = ResultTestNode(backgroundScope, "awaiting")

        val deferred = async { navFlow.pushAndAwaitResult(awaitingNode) }
        advanceUntilIdle()

        awaitingNode.emitOkValue("done")

        val result = deferred.await()
        assertIs<KmposableResult.Ok<String>>(result)
        assertEquals("done", result.value)
        assertSame(navFlow.navState.value.root, navFlow.currentTopNode())
    }

    @Test
    fun updateTopNodeRunsOnlyWhenTypeMatches() = runTest {
        val navFlow = navFlowWithRoot()

        assertTrue(navFlow.updateTopNode<PassiveNode> { onEvent(Unit) })

        navFlow.push(ResultTestNode(backgroundScope, "awaiting"))

        assertFalse(navFlow.updateTopNode<PassiveNode> { onEvent(Unit) })
    }

    @Test
    fun updateTopNodeRequiresStartedFlow() = runTest {
        val navFlow = NavFlow<Unit, DefaultStackEntry<Unit>>(
            appScope = backgroundScope,
            rootNode = PassiveNode(backgroundScope, "root")
        )

        assertFailsWith<IllegalStateException> {
            navFlow.updateTopNode<PassiveNode> { onEvent(Unit) }
        }

        navFlow.start()
        navFlow.dispose()

        assertFailsWith<IllegalStateException> {
            navFlow.updateTopNode<PassiveNode> { onEvent(Unit) }
        }
    }

    private fun TestScope.navFlowWithRoot(): NavFlow<Unit, DefaultStackEntry<Unit>> {
        val navFlow = NavFlow<Unit, DefaultStackEntry<Unit>>(
            appScope = backgroundScope,
            rootNode = PassiveNode(backgroundScope, "root")
        )
        navFlow.start()
        return navFlow
    }

    private class PassiveNode(
        scope: CoroutineScope,
        id: String
    ) : StatefulNode<Unit, Unit, Unit>(scope, Unit, id = id) {
        override fun onEvent(event: Unit) = Unit
    }

    private class ResultTestNode(
        private val parentScope: CoroutineScope,
        id: String
    ) : ResultfulStatefulNode<Unit, Unit, Unit, String>(
        parentScope = parentScope,
        initialState = Unit,
        id = id
    ) {
        override fun onEvent(event: Unit) = Unit

        fun emitOkValue(value: String) {
            parentScope.launch { emitOk(value) }
        }
    }
}

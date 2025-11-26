package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import kotlinx.coroutines.flow.firstOrNull

/**
 * Pushes an existing [ResultNode], awaits its first result, and optionally pops the node.
 *
 * @param autoPop When true (default), the node is popped in `finally` after the first result.
 *                When false, the caller is responsible for managing the stack.
 * @return The first emitted result, or [KmposableResult.Canceled] if no result was emitted.
 */
suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any> NavFlow<OUT, ENTRY>.pushAndAwaitResult(
    node: ResultNode<RESULT>,
    autoPop: Boolean = true
): KmposableResult<RESULT> {
    @Suppress("UNCHECKED_CAST")
    val typedNode = node as dev.goquick.kmposable.core.Node<*, *, OUT>
    push(typedNode)
    val result = try {
        node.result.firstOrNull()
    } finally {
        if (autoPop && isStarted()) {
            pop()
        }
    }
    return result ?: KmposableResult.Canceled
}

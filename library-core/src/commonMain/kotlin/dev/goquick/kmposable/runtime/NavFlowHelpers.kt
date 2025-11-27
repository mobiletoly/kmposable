package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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

/**
 * Creates, pushes, awaits, and optionally pops a [ResultNode] produced by [factory].
 *
 * Use this to avoid manual `push`/`launch` boilerplate in host code. The [onResult] callback
 * is invoked with the first emitted result before the value is returned to the caller.
 */
suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any> NavFlow<OUT, ENTRY>.pushAndAwaitResult(
    factory: () -> ResultNode<RESULT>,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): KmposableResult<RESULT> {
    val node = factory()
    val result = pushAndAwaitResult(node, autoPop)
    onResult(result)
    return result
}

/**
 * Launches a coroutine that pushes a result node from [factory], awaits the first result,
 * optionally pops it, and forwards the result to [onResult].
 *
 * This is a small ergonomic wrapper around [pushAndAwaitResult] for Compose/host code that
 * already has a [CoroutineScope].
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any> CoroutineScope.launchPushAndAwaitResult(
    navFlow: NavFlow<OUT, ENTRY>,
    factory: () -> ResultNode<RESULT>,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): Job = launch {
    navFlow.pushAndAwaitResult(factory, autoPop, onResult)
}

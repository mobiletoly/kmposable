package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.ResultOnlyNode
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Pushes an existing [ResultNode], awaits its first result, and optionally pops the node.
 *
 * @param autoPop When true (default), the node is removed after the first result if it is still
 * present on the stack. When false, the caller is responsible for managing the stack.
 * @return The first emitted result, or [KmposableResult.Canceled] if the node leaves the stack
 * before producing one.
 */
suspend fun <
    OUT : Any,
    ENTRY : KmposableStackEntry<OUT>,
    RESULT : Any,
    NODE
    > NavFlow<OUT, ENTRY>.pushAndAwaitResult(
    node: NODE,
    autoPop: Boolean = true
) : KmposableResult<RESULT>
    where NODE : Node<*, *, OUT>,
          NODE : ResultNode<RESULT> = awaitResultNode(node, node, autoPop)

/**
 * Creates, pushes, awaits, and optionally pops a [ResultNode] produced by [factory].
 *
 * Use this to avoid manual `push`/`launch` boilerplate in host code. The [onResult] callback
 * is invoked with the first emitted result before the value is returned to the caller.
 */
suspend fun <
    OUT : Any,
    ENTRY : KmposableStackEntry<OUT>,
    RESULT : Any,
    NODE
    > NavFlow<OUT, ENTRY>.pushAndAwaitResult(
    factory: () -> NODE,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): KmposableResult<RESULT>
    where NODE : Node<*, *, OUT>,
          NODE : ResultNode<RESULT> {
    val node = factory()
    val result = pushAndAwaitResult(node, autoPop)
    onResult(result)
    return result
}

/**
 * Variant for result-only nodes (OUTPUT = Nothing) so callers don't need to mention the flow's OUT.
 */
suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any, NODE> NavFlow<OUT, ENTRY>.pushAndAwaitResultOnly(
    factory: () -> NODE,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): KmposableResult<RESULT>
    where NODE : ResultOnlyNode<*, *, RESULT> {
    val node = factory()
    @Suppress("UNCHECKED_CAST")
    val typedNode = node as Node<*, *, OUT>
    val result = awaitResultNode(typedNode, node, autoPop)
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
fun <
    OUT : Any,
    ENTRY : KmposableStackEntry<OUT>,
    RESULT : Any,
    NODE
    > CoroutineScope.launchPushAndAwaitResult(
    navFlow: NavFlow<OUT, ENTRY>,
    factory: () -> NODE,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): Job
    where NODE : Node<*, *, OUT>,
          NODE : ResultNode<RESULT> = launch {
    navFlow.pushAndAwaitResult(factory, autoPop, onResult)
}

/**
 * Variant for result-only nodes (OUTPUT = Nothing) so callers don't need to mention the flow's OUT.
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any, NODE> CoroutineScope.launchPushAndAwaitResultOnly(
    navFlow: NavFlow<OUT, ENTRY>,
    factory: () -> NODE,
    autoPop: Boolean = true,
    onResult: (KmposableResult<RESULT>) -> Unit = {}
): Job
    where NODE : ResultOnlyNode<*, *, RESULT> = launch {
    navFlow.pushAndAwaitResultOnly(factory, autoPop, onResult)
}

/**
 * Runs [block] against the current top node when it matches [NODE], otherwise returns `null`.
 *
 * This is a safer alternative to casting [NavFlow.currentTopNode] manually in UI/tests.
 */
inline fun <reified NODE, R> NavFlow<*, *>.withTopNode(
    block: NODE.() -> R
): R? where NODE : Node<*, *, *> {
    val node = currentTopNode() as? NODE ?: return null
    return block(node)
}

/**
 * Runs [block] against the current top node only when it matches [NODE].
 *
 * Returns `false` instead of throwing when the visible node is not the expected type.
 */
inline fun <reified NODE> NavFlow<*, *>.updateTopNode(
    block: NODE.() -> Unit
): Boolean where NODE : Node<*, *, *> {
    check(isStarted()) {
        "NavFlow is not started or has already been disposed. Call start() before updateTopNode()."
    }
    val node = currentTopNode() as? NODE ?: return false
    node.block()
    return true
}

private suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, RESULT : Any> NavFlow<OUT, ENTRY>.awaitResultNode(
    stackNode: Node<*, *, OUT>,
    resultNode: ResultNode<RESULT>,
    autoPop: Boolean
): KmposableResult<RESULT> = coroutineScope {
    push(stackNode)

    val resultDeferred = async {
        resultNode.result.firstOrNull()
    }
    val removalDeferred = async {
        navState.firstOrNull { state -> state.stack.none { it.node === stackNode } }
        KmposableResult.Canceled
    }

    val outcome = select<KmposableResult<RESULT>> {
        resultDeferred.onAwait { it ?: KmposableResult.Canceled }
        removalDeferred.onAwait { it }
    }

    if (autoPop && isStarted()) {
        val currentStack = navState.value.stack
        if (currentStack.any { it.node === stackNode }) {
            if (currentStack.last().node === stackNode) {
                pop()
            } else {
                popTo(stackNode, inclusive = true)
            }
        }
    }

    if (resultDeferred.isActive) resultDeferred.cancelAndJoin()
    if (removalDeferred.isActive) removalDeferred.cancelAndJoin()

    outcome
}

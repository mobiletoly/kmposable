package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.KmposableStackEntry

/**
 * Runs [block] and emits results to [onSuccess] or [onError]. Useful for scripts that need to
 * update a node's loading/error state while performing repository work.
 */
suspend inline fun <T> NavFlowScriptScope<*, *>.runCatchingNodeCall(
    crossinline onLoading: () -> Unit,
    crossinline onSuccess: (T) -> Unit,
    crossinline onError: (Throwable) -> Unit,
    crossinline block: suspend () -> T
) {
    onLoading()
    runCatching { block() }
        .onSuccess { onSuccess(it) }
        .onFailure { onError(it) }
}

/**
 * Waits until [mapper] returns a non-null value for the next output and returns it.
 */
suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, T : Any> NavFlowScriptScope<OUT, ENTRY>.awaitMappedOutput(
    mapper: (OUT) -> T?
): T {
    while (true) {
        val next = awaitOutput<OUT> { true }
        val mapped = mapper(next)
        if (mapped != null) return mapped
    }
}

/**
 * Pushes [node], runs [block], then pops the node when [block] completes.
 */
suspend fun <NodeT : Node<*, *, OUT>, OUT : Any, ENTRY : KmposableStackEntry<OUT>, R> NavFlowScriptScope<OUT, ENTRY>.withNode(
    node: NodeT,
    block: suspend NodeT.() -> R
): R {
    navFlow.push(node)
    return try {
        block(node)
    } finally {
        navFlow.pop()
    }
}

/**
 * Pushes a node created by [factory], runs [block], and pops the node when [block] completes.
 */
suspend inline fun <NodeT : Node<*, *, OUT>, OUT : Any, ENTRY : KmposableStackEntry<OUT>, R> NavFlowScriptScope<OUT, ENTRY>.withNode(
    factory: () -> NodeT,
    crossinline block: suspend NodeT.() -> R
): R {
    val node = factory()
    navFlow.push(node)
    return try {
        block(node)
    } finally {
        navFlow.pop()
    }
}

/**
 * Pushes a node created by [factory], waits for an output that [mapper] can convert to [T],
 * and returns the mapped value once the node is popped.
 */
suspend fun <NodeT : Node<*, *, OUT>, OUT : Any, ENTRY : KmposableStackEntry<OUT>, T : Any> NavFlowScriptScope<OUT, ENTRY>.pushAndAwait(
    factory: () -> NodeT,
    mapper: (OUT) -> T?
): T {
    var result: T? = null
    withNode(factory) {
        result = awaitMappedOutput(mapper)
    }
    return result!!
}

/** DSL builder used by [awaitOutputCase] to describe typed orchestration branches. */
class OutputCaseBuilder<OUT : Any, R> internal constructor() {
    @PublishedApi internal val matchers = mutableListOf<(OUT) -> R?>()
    @PublishedApi internal var fallback: ((OUT) -> R?)? = null

    /** Handles outputs of type [T]. */
    inline fun <reified T : OUT> on(noinline handler: (T) -> R) {
        matchers += { output -> if (output is T) handler(output) else null }
    }

    /** Handles outputs matching [predicate]. */
    fun match(predicate: (OUT) -> Boolean, handler: (OUT) -> R) {
        matchers += { output -> if (predicate(output)) handler(output) else null }
    }

    /** Fallback branch invoked when no other matcher handled the output. */
    fun otherwise(handler: (OUT) -> R) {
        fallback = handler
    }

    internal fun map(output: OUT): R? {
        matchers.forEach { matcher ->
            val mapped = matcher(output)
            if (mapped != null) return mapped
        }
        return fallback?.invoke(output)
    }
}

/**
 * Awaits the next output that matches one of the [builder] branches and returns the mapped value.
 */
suspend fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>, R> NavFlowScriptScope<OUT, ENTRY>.awaitOutputCase(
    builder: OutputCaseBuilder<OUT, R>.() -> Unit
): R {
    val caseBuilder = OutputCaseBuilder<OUT, R>().apply(builder)
    return awaitMappedOutput { output -> caseBuilder.map(output) }
}

/**
 * Pushes a node using [factory], observes outputs until [mapper] returns a value, and pops the node.
 */
suspend fun <NodeT : Node<*, *, OUT>, OUT : Any, Result : Any> NavFlowScriptScope<OUT, *>.pushForResult(
    factory: () -> NodeT,
    mapper: (OUT) -> Result?
): Result {
    val node = factory()
    navFlow.push(node)
    return try {
        awaitMappedOutput(mapper)
    } finally {
        navFlow.pop()
    }
}

/**
 * Runs [block] against the current top node, throwing if it is not of type [T].
 */
suspend inline fun <OUT : Any, reified T : Node<*, *, OUT>> NavFlowScriptScope<OUT, *>.updateTopNode(
    noinline block: suspend T.() -> Unit
) {
    val node = navFlow.currentTopNode()
    val typed = node as? T
        ?: error("Expected top node of type ${T::class.simpleName}, but was ${node::class.simpleName}")
    block(typed)
}

/**
 * Retrieves the current top node as [T], executes [block], and returns its result.
 */
suspend inline fun <OUT : Any, reified T : Node<*, *, OUT>, R> NavFlowScriptScope<OUT, *>.withTopNode(
    noinline block: suspend T.() -> R
): R {
    val node = navFlow.currentTopNode()
    val typed = node as? T
        ?: error("Expected top node of type ${T::class.simpleName}, but was ${node::class.simpleName}")
    return block(typed)
}

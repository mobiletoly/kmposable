package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.KmposableNavigator
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.jvm.JvmName

/**
 * Primary scripting surface for driving a [NavFlow] sequentially. Prefer using its helpers
 * (showRoot, pushNode, awaitOutput*, trace) instead of mutating the navigator directly unless you
 * have an advanced use case.
 */
interface NavFlowScriptScope<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    /** Flow being orchestrated. */
    val navFlow: NavFlow<OUT, ENTRY>

    /** Underlying navigator that powers this flow. */
    val navigator: KmposableNavigator<OUT, ENTRY>

    /** Convenience access to the navigation state. */
    val navState: StateFlow<KmposableNavState<OUT, ENTRY>>
        get() = navFlow.navState

    /** Convenience hook to mutate the navigator stack. */
    fun showNode(block: KmposableNavigator<OUT, ENTRY>.() -> Unit) {
        navFlow.mutateNavigator { navigator.block() }
    }

    /** Replaces the entire stack with [node]. */
    fun showRoot(factory: () -> Node<*, *, OUT>) {
        val entry = createEntry(factory())
        navFlow.mutateNavigator { it.replaceAll(entry) }
    }

    /** Pushes a new node created by [factory]. */
    fun pushNode(factory: () -> Node<*, *, OUT>) {
        val entry = createEntry(factory())
        navFlow.mutateNavigator { it.push(entry) }
    }

    /** Replaces the top entry with a node from [factory]. */
    fun replaceTop(factory: () -> Node<*, *, OUT>) {
        val entry = createEntry(factory())
        navFlow.mutateNavigator { it.replaceTop(entry) }
    }

    /** Emits a trace message for debugging if tracing is enabled. */
    fun trace(message: () -> String)

    /** Converts a [Node] instance into a stack entry compatible with this NavFlow. */
    fun createEntry(node: Node<*, *, OUT>): ENTRY

    /**
     * Suspends until an output matching [predicate] arrives.
     * Only outputs emitted after this call starts collecting are considered.
     */
    suspend fun <T : OUT> awaitOutput(predicate: (OUT) -> Boolean): T

}

/** Convenience helper that waits for the next output of type [T]. */
suspend inline fun <reified T : Any> NavFlowScriptScope<*, *>.awaitOutputOfType(): T {
    @Suppress("UNCHECKED_CAST")
    val typedScope = this as NavFlowScriptScope<Any, *>
    return typedScope.awaitOutput { it is T } as T
}

/**
 * Launches a coroutine-backed script that can drive this [NavFlow] sequentially.
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlow<OUT, ENTRY>.launchNavFlowScript(
    scope: CoroutineScope,
    onTrace: ((String) -> Unit)? = null,
    script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job {
    val scriptScope = DefaultNavFlowScriptScope(this, scope, onTrace)
    return scope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            scriptScope.script()
        } finally {
            scriptScope.dispose()
        }
    }
}

/**
 * Readability alias for [launchNavFlowScript].
 */
@JvmName("runScriptForNavFlow")
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlow<OUT, ENTRY>.runScript(
    scope: CoroutineScope,
    onTrace: ((String) -> Unit)? = null,
    block: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job = launchNavFlowScript(scope, onTrace, block)

typealias SimpleNavFlowScriptScope<OUT> = NavFlowScriptScope<OUT, DefaultStackEntry<OUT>>

/**
 * Convenience alias for running scripts against [SimpleNavFlow].
 */
@JvmName("runScriptForSimpleNavFlow")
fun <OUT : Any> SimpleNavFlow<OUT>.runScript(
    scope: CoroutineScope,
    onTrace: ((String) -> Unit)? = null,
    block: suspend SimpleNavFlowScriptScope<OUT>.() -> Unit
): Job = launchNavFlowScript(scope, onTrace, block)

private class DefaultNavFlowScriptScope<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    override val navFlow: NavFlow<OUT, ENTRY>,
    parentScope: CoroutineScope,
    private val onTrace: ((String) -> Unit)?
) : NavFlowScriptScope<OUT, ENTRY> {
    override val navigator: KmposableNavigator<OUT, ENTRY> = navFlow.navigatorHandle()
    private val outputChannel = Channel<OUT>(capacity = Channel.UNLIMITED)
    private val collectorJob: Job = parentScope.launch(start = CoroutineStart.UNDISPATCHED) {
        navFlow.outputs.collect { outputChannel.send(it) }
    }

    override fun createEntry(node: Node<*, *, OUT>): ENTRY = navFlow.entryFor(node)

    override fun trace(message: () -> String) {
        onTrace?.invoke(message())
    }

    override suspend fun <T : OUT> awaitOutput(predicate: (OUT) -> Boolean): T {
        while (true) {
            val result = outputChannel.receiveCatching()
            val next = result.getOrNull() ?: throw CancellationException(
                "NavFlowScriptScope outputs channel closed",
                result.exceptionOrNull()
            )
            if (predicate(next)) {
                @Suppress("UNCHECKED_CAST")
                return next as T
            }
        }
    }

    fun dispose() {
        collectorJob.cancel()
        outputChannel.cancel()
    }
}

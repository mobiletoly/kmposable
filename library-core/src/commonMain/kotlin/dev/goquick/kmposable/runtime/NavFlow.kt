package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.KmposableNavigator
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Primary headless runtime that drives a stack of [Node]s. UI adapters
 * observe [navState] to render tree snapshots, feed user events via [sendEvent], and react to
 * business outputs using [outputs]. All lifecycle and navigation semantics live here so the same
 * runtime can power tests, CLIs, or UI shells. Call [start] once the surrounding environment is
 * ready, and [dispose] when the flow is torn down; both methods are idempotent.
 */
open class NavFlow<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    private val appScope: CoroutineScope,
    rootNode: Node<*, *, OUT>,
    navigatorFactory: (ENTRY) -> KmposableNavigator<OUT, ENTRY> = { entry ->
        KmposableStackNavigator(entry)
    }
) {

    /** Backing navigator implementation used to mutate the stack. */
    protected val navigator: KmposableNavigator<OUT, ENTRY> = navigatorFactory(createEntry(rootNode))

    /** State flow that renderers/adapters observe to render the current node. */
    val navState: StateFlow<KmposableNavState<OUT, ENTRY>> = navigator.state

    /** Tracks active output collection jobs keyed by runtime-unique node tokens. */
    private val outputCollectors = mutableMapOf<NodeToken<OUT>, Job>()
    private val _outputs = MutableSharedFlow<OUT>(extraBufferCapacity = 16)

    /** Hot stream of outputs emitted by any node in this runtime. Buffer size: 16. */
    val outputs: Flow<OUT> = _outputs.asSharedFlow()

    private var started = false

    /** Starts the runtime if it has not been started yet. Safe to call multiple times. */
    fun start() {
        if (started) return
        started = true
        attachNode(currentTopNode())
    }

    /** Returns the current top node (helpful for tests/adapters). */
    fun currentTopNode(): Node<*, *, OUT> = navState.value.top

    /** Pushes a new node and begins observing its outputs. */
    fun push(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        navigator.push(entry)
        attachNode(node)
    }

    /**
     * Pops the stack if possible and tears down lifecycle observers for the removed node.
     *
     * @return true if a node was removed, false if already at the root.
     */
    fun pop(): Boolean {
        ensureStarted()
        val removed = navigator.pop()
        removed?.let { detachNode(it.node) }
        return removed != null
    }

    /** Replaces the stack with a single node (used for flow resets/onboarding swap). */
    fun replaceAll(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        val removed = navigator.replaceAll(entry)
        removed.forEach { detachNode(it.node) }
        attachNode(node)
    }

    /** Replaces only the top-most entry. */
    fun replaceTop(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        navigator.replaceTop(entry)?.let { detachNode(it.node) }
        attachNode(node)
    }

    /** Pops every entry above the root element. */
    fun popAll() {
        ensureStarted()
        navigator.popAll().forEach { detachNode(it.node) }
    }

    /** Pops nodes until [target] is on top, optionally removing [target] as well. */
    fun popTo(target: Node<*, *, OUT>, inclusive: Boolean = false) {
        ensureStarted()
        val targetEntry = findEntryForNode(target) ?: return
        navigator.popTo(targetEntry, inclusive).forEach { detachNode(it.node) }
    }

    /** Indicates whether the stack has more than one entry. */
    fun canPop(): Boolean {
        ensureStarted()
        return navState.value.size > 1
    }

    /** Returns to the root node. */
    fun popToRoot() {
        ensureStarted()
        val root = navState.value.root
        popTo(root, inclusive = false)
    }

    /**
     * Injects [event] into the currently visible node. The caller must ensure the payload matches
     * the node's expected event type to avoid a [ClassCastException].
     */
    fun sendEvent(event: Any) {
        ensureStarted()
        dispatchEvent(currentTopNode(), event)
    }

    /**
     * Stops observing all node outputs and triggers detach hooks for every node. Safe to call even
     * if [start] was never invoked.
     */
    fun dispose() {
        if (!started) return
        navState.value.stack.forEach { detachNode(it.node) }
        navigator.popAll()
        started = false
    }

    protected open fun onNodeOutput(node: Node<*, *, OUT>, output: OUT) = Unit

    /** Called whenever a node becomes part of the stack so subclasses can hook additional logic. */
    protected open fun attachNode(node: Node<*, *, OUT>) {
        (node as? LifecycleAwareNode)?.onAttach()
        observeNodeOutputs(node)
    }

    /** Called right before a node leaves the stack. */
    protected open fun detachNode(node: Node<*, *, OUT>) {
        stopObservingNode(node)
        (node as? LifecycleAwareNode)?.onDetach()
    }

    protected open fun observeNodeOutputs(node: Node<*, *, OUT>) {
        val token = NodeToken(node)
        if (outputCollectors.containsKey(token)) return
        val job = appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            node.outputs.collect { output ->
                emitRuntimeOutput(output)
                onNodeOutput(node, output)
            }
        }
        outputCollectors[token] = job
    }

    private fun emitRuntimeOutput(output: OUT) {
        if (!_outputs.tryEmit(output)) {
            appScope.launch { _outputs.emit(output) }
        }
    }

    private fun findEntryForNode(node: Node<*, *, OUT>): ENTRY? =
        navState.value.stack.firstOrNull { it.node == node }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchEvent(node: Node<*, *, OUT>, event: Any) {
        (node as Node<Any, Any, OUT>).onEvent(event)
    }

    private fun stopObservingNode(node: Node<*, *, OUT>) {
        outputCollectors.remove(NodeToken(node))?.cancel()
    }

    private fun ensureStarted() {
        check(started) { "NavFlow must be started via start() before use." }
    }

    /** Creates the stack entry representation for [node]. Subclasses can attach extra metadata. */
    protected open fun createEntry(node: Node<*, *, OUT>): ENTRY {
        @Suppress("UNCHECKED_CAST")
        return DefaultStackEntry(node) as ENTRY
    }
}

typealias SimpleNavFlow<OUT> = NavFlow<OUT, DefaultStackEntry<OUT>>

/** Lightweight identity token used to key maps safely without depending on equals/hashCode. */
private data class NodeToken<OUT : Any>(val node: Node<*, *, OUT>)

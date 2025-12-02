/*
 * Copyright 2025 Toly Pochkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.KmposableNavigator
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackNavigator
import dev.goquick.kmposable.core.logging.NavFlowLogger
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
    },
    private val logger: NavFlowLogger<OUT>? = null
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

    /** True when [start] has been invoked and the runtime is active. */
    fun isStarted(): Boolean = started

    /** Starts the runtime if it has not been started yet. Safe to call multiple times. */
    fun start() {
        if (started) return
        started = true
        attachNode(currentTopNode())
        logger?.onStackChanged(navState.value)
    }

    /** Returns the current top node (helpful for tests/adapters). */
    fun currentTopNode(): Node<*, *, OUT> = navState.value.top

    /** Pushes a new node and begins observing its outputs. */
    fun push(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        navigator.push(entry)
        attachNode(node)
        logger?.onStackChanged(navState.value)
    }

    /**
     * Safe variant of [push] that returns false without throwing if the flow is not started.
     */
    fun pushIfStarted(node: Node<*, *, OUT>): Boolean {
        if (!started) return false
        push(node)
        return true
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
        if (removed != null) logger?.onStackChanged(navState.value)
        return removed != null
    }

    /**
     * Safe variant of [pop] that returns false without throwing if the flow is not started.
     */
    fun popIfStarted(): Boolean {
        if (!started) return false
        return pop()
    }

    /** Replaces the stack with a single node (used for flow resets/onboarding swap). */
    fun replaceAll(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        val removed = navigator.replaceAll(entry)
        removed.forEach { detachNode(it.node) }
        attachNode(node)
        logger?.onStackChanged(navState.value)
    }

    /** Replaces only the top-most entry. */
    fun replaceTop(node: Node<*, *, OUT>) {
        ensureStarted()
        val entry = createEntry(node)
        navigator.replaceTop(entry)?.let { detachNode(it.node) }
        attachNode(node)
        logger?.onStackChanged(navState.value)
    }

    /** Pops every entry above the root element. */
    fun popAll() {
        ensureStarted()
        navigator.popAll().forEach { detachNode(it.node) }
        logger?.onStackChanged(navState.value)
    }

    /** Pops nodes until [target] is on top, optionally removing [target] as well. */
    fun popTo(target: Node<*, *, OUT>, inclusive: Boolean = false) {
        ensureStarted()
        val targetEntry = findEntryForNode(target) ?: return
        navigator.popTo(targetEntry, inclusive).forEach { detachNode(it.node) }
        logger?.onStackChanged(navState.value)
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
        logger?.onAttach(node)
    }

    /** Called right before a node leaves the stack. */
    protected open fun detachNode(node: Node<*, *, OUT>) {
        stopObservingNode(node)
        (node as? LifecycleAwareNode)?.onDetach()
        logger?.onDetach(node)
    }

    protected open fun observeNodeOutputs(node: Node<*, *, OUT>) {
        val token = NodeToken(node)
        if (outputCollectors.containsKey(token)) return
        val job = appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            node.outputs.collect { output ->
                logger?.onOutput(node, output)
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

    internal fun navigatorHandle(): KmposableNavigator<OUT, ENTRY> = navigator

    internal fun entryFor(node: Node<*, *, OUT>): ENTRY = createEntry(node)

    internal fun mutateNavigator(mutator: (KmposableNavigator<OUT, ENTRY>) -> Unit) {
        val beforeNodes = navState.value.stack.map { it.node }
        mutator(navigator)
        val afterNodes = navState.value.stack.map { it.node }

        beforeNodes.filterNot { it in afterNodes }.forEach { detachNode(it) }
        afterNodes.filterNot { it in beforeNodes }.forEach { attachNode(it) }
    }
}

typealias SimpleNavFlow<OUT> = NavFlow<OUT, DefaultStackEntry<OUT>>

/** Lightweight identity token used to key maps safely without depending on equals/hashCode. */
private data class NodeToken<OUT : Any>(val node: Node<*, *, OUT>)

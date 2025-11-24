package dev.goquick.kmposable.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Convenience base class that wraps another [Node] and delegates all behavior to it.
 * Subclasses may override any member to tweak behavior without re-implementing boilerplate.
 *
 * Example:
 *
 * ```
 * class TrackingNode(
 *     wrapped: Node<State, Event, Output>,
 *     private val tracker: (Output) -> Unit
 * ) : DelegatingNode<State, Event, Output>(wrapped) {
 *     override val outputs: Flow<Output> = super.outputs.onEach(tracker)
 * }
 * ```
 */
open class DelegatingNode<STATE : Any, EVENT : Any, OUTPUT : Any>(
    private val node: Node<STATE, EVENT, OUTPUT>
) : Node<STATE, EVENT, OUTPUT>, LifecycleAwareNode {

    override val state: StateFlow<STATE> get() = node.state

    override fun onEvent(event: EVENT) {
        node.onEvent(event)
    }

    override val outputs: Flow<OUTPUT> get() = node.outputs

    override fun onAttach() {
        (node as? LifecycleAwareNode)?.onAttach()
    }

    override fun onDetach() {
        (node as? LifecycleAwareNode)?.onDetach()
    }
}

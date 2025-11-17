package dev.goquick.kmposable.core

/**
 * Optional contract for nodes that want to know when they enter or leave the navigation tree.
 *
 * A node is *attached* when it becomes part of the active stack – either because it is pushed,
 * becomes the new root via `replaceAll`, or the runtime starts for the first time. A node is
 * *detached* every time it leaves the stack: pop, replace, popAll, popTo, or runtime disposal.
 * Attach/detach can therefore be invoked multiple times during a node's lifetime if the same
 * instance is reintroduced.
 */
interface LifecycleAwareNode {
    /** Called whenever the node transitions from not-present to present on the stack. */
    fun onAttach() {}

    /** Called right before the node is removed from the stack (pop/replace/dispose). */
    fun onDetach() {}
}

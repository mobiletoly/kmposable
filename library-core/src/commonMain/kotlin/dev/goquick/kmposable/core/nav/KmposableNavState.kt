package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.StatefulNode

/** Represents a typed stack entry that wraps a [Node]. */
interface KmposableStackEntry<OUT : Any> {
    /** Concrete node instance backing this entry. */
    val node: Node<*, *, OUT>
    /** Optional logical identifier (used for logging/tests). */
    val tag: String?
}

/** Default stack entry implementation that simply wraps a [Node]. */
data class DefaultStackEntry<OUT : Any>(
    override val node: Node<*, *, OUT>,
    override val tag: String? = node.resolveNodeTag()
) : KmposableStackEntry<OUT>

/**
 * Immutable snapshot of the navigator stack. Entries are ordered from root (index 0) to the
 * current top. The stack is guaranteed to contain at least one element for the lifetime of a
 * runtime, which simplifies rendering code (no “empty stack” branch).
 */
data class KmposableNavState<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    /** Ordered collection of entries, bottom (index 0) to top. */
    val stack: List<ENTRY>
) {
    init {
        require(stack.isNotEmpty()) { "NavState.stack must not be empty" }
    }

    /** Convenience accessor for the root entry. */
    val rootEntry: ENTRY get() = stack.first()

    /** Convenience accessor for the top-most entry. */
    val topEntry: ENTRY get() = stack.last()

    /** Shortcut to the root node instance (bottom of the stack). */
    val root: Node<*, *, OUT> get() = rootEntry.node

    /** Shortcut to the top node instance (currently rendered node). */
    val top: Node<*, *, OUT> get() = topEntry.node

    val size: Int get() = stack.size
}

internal fun Node<*, *, *>.resolveNodeTag(): String? =
    (this as? StatefulNode<*, *, *>)?.id ?: this::class.simpleName

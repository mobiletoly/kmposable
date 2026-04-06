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
package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.StatefulNode
import kotlin.random.Random

/**
 * Represents a typed stack entry that wraps a [Node].
 *
 * This remains public in `0.3.x` as an advanced headless/runtime customization point for tests,
 * scripts, saveable-state identity, and host metadata. It is not part of the primary Navigation 3
 * integration story.
 */
interface KmposableStackEntry<OUT : Any> {
    /** Concrete node instance backing this entry. */
    val node: Node<*, *, OUT>
    /** Optional logical identifier (used for logging/tests). */
    val tag: String?
    /**
     * Stable key used by Compose hosts to preserve saveable subtree state for this entry while it
     * remains on the runtime stack. Custom value-based entries should include this identity in
     * their equality semantics if replacements need to be observable through [KmposableNavState].
     */
    val saveableStateKey: String
        get() = buildInstanceScopedSaveableStateKey(node = node, entry = this)
}

enum class Presentation { Primary, Overlay }

/**
 * Optional marker for nodes to hint their presentation style to hosts.
 * Defaults to [Presentation.Primary]; override to [Presentation.Overlay]
 * when a node should render as an overlay on top of the primary content.
 */
interface PresentationAware {
    val presentation: Presentation get() = Presentation.Primary
}

/** Convenience helper to check if an object declares overlay presentation. */
fun Any?.isOverlayPresentation(): Boolean =
    (this as? PresentationAware)?.presentation == Presentation.Overlay

/** Default stack entry implementation that simply wraps a [Node]. */
data class DefaultStackEntry<OUT : Any>(
    override val node: Node<*, *, OUT>,
    override val tag: String? = node.resolveNodeTag(),
    override val saveableStateKey: String = buildDefaultSaveableStateKey(tag, node)
) : KmposableStackEntry<OUT>

/**
 * Immutable snapshot of the navigator stack. Entries are ordered from root (index 0) to the
 * current top. The stack is guaranteed to contain at least one element for the lifetime of a
 * runtime, which simplifies rendering code (no “empty stack” branch).
 *
 * The snapshot remains public in `0.3.x` because renderers, tests, and scripts need direct,
 * typed access to the runtime stack without binding core to a specific UI router.
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

private fun buildInstanceScopedSaveableStateKey(
    node: Node<*, *, *>,
    entry: Any
): String {
    val prefix = node::class.simpleName ?: "node"
    val stableSuffix = stableIdentityHashCode(entry).toUInt().toString(16)
    return "$prefix@$stableSuffix"
}

private fun buildDefaultSaveableStateKey(tag: String?, node: Node<*, *, *>): String {
    val prefix = saveableStateKeyPrefix(tag, node)
    val randomSuffix = Random.nextLong().toULong().toString(16)
    return "$prefix@$randomSuffix"
}

private fun saveableStateKeyPrefix(tag: String?, node: Node<*, *, *>): String =
    tag ?: node.resolveNodeTag() ?: "node"

internal expect fun stableIdentityHashCode(instance: Any): Int

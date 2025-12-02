package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest

/**
 * Remembers a [Node] instance tied to the composition. Useful for nodes that are *not* managed by a
 * [NavFlow] (e.g., dashboard list nodes hosted locally inside a screen).
 *
 * The returned node is created once per [key] and reused across recompositions. Consumers can pair
 * this with [NodeHost] for lifecycle + output wiring.
 */
@Composable
fun <STATE : Any, EVENT : Any, OUTPUT : Any, NODE> rememberNode(
    key: Any? = Unit,
    parentScope: CoroutineScope = rememberCoroutineScope(),
    factory: (CoroutineScope) -> NODE
): NODE where NODE : Node<STATE, EVENT, OUTPUT> {
    return remember(key, parentScope) { factory(parentScope) }
}

/**
 * Convenience host for standalone nodes. Handles lifecycle hooks, optional output collection, and
 * exposes [state] + [onEvent] to [content].
 *
 * Do not use this for nodes already attached to a [NavFlow] (they are attached/detached there).
 */
@Composable
fun <STATE : Any, EVENT : Any, OUTPUT : Any, NODE> NodeHost(
    node: NODE,
    autoAttach: Boolean = true,
    onOutput: (suspend (OUTPUT) -> Unit)? = null,
    content: @Composable (state: STATE, onEvent: (EVENT) -> Unit, node: NODE) -> Unit
) where NODE : Node<STATE, EVENT, OUTPUT> {
    if (autoAttach) {
        DisposableEffect(node) {
            (node as? LifecycleAwareNode)?.onAttach()
            onDispose { (node as? LifecycleAwareNode)?.onDetach() }
        }
    }

    onOutput?.let { handler ->
        LaunchedEffect(node, handler) {
            node.outputs.collectLatest { handler(it) }
        }
    }

    val state by node.state.collectAsState()
    content(state, node::onEvent, node)
}

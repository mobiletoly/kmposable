package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.core.Node
import kotlin.reflect.KClass

/** Registry of Composable renderers keyed by node type. */
class NodeRenderer<OUT : Any> internal constructor(
    private val delegates: Map<KClass<out Node<*, *, OUT>>, @Composable (Node<*, *, OUT>) -> Unit>,
    private val fallback: (@Composable (Node<*, *, OUT>) -> Unit)? = null
) {
    /** Returns true if a renderer has been registered for this node type. */
    fun canRender(node: Node<*, *, OUT>): Boolean = findRenderer(node) != null || fallback != null

    @Composable
    internal fun Render(node: Node<*, *, OUT>) {
        val renderer = findRenderer(node)
            ?: fallback ?: error("No renderer registered for ${node::class}")
        renderer(node)
    }

    private fun findRenderer(node: Node<*, *, OUT>): (@Composable (Node<*, *, OUT>) -> Unit)? {
        val kClass = node::class
        delegates[kClass]?.let { return it }
        return delegates.entries.firstOrNull { entry ->
            entry.key != kClass && entry.key.isInstance(node)
        }?.value
    }
}

class NodeRendererBuilder<OUT : Any> internal constructor() {
    @PublishedApi
    internal val delegates = LinkedHashMap<KClass<out Node<*, *, OUT>>, @Composable (Node<*, *, OUT>) -> Unit>()
    @PublishedApi
    internal var fallback: (@Composable (Node<*, *, OUT>) -> Unit)? = null

    inline fun <reified T : Node<*, *, OUT>> register(noinline renderer: @Composable (T) -> Unit) {
        delegates[T::class] = { node -> renderer(node as T) }
    }

    fun fallback(renderer: @Composable (Node<*, *, OUT>) -> Unit) {
        fallback = renderer
    }

    internal fun build(): NodeRenderer<OUT> = NodeRenderer(delegates.toMap(), fallback)
}

fun <OUT : Any> nodeRenderer(builder: NodeRendererBuilder<OUT>.() -> Unit): NodeRenderer<OUT> {
    return NodeRendererBuilder<OUT>().apply(builder).build()
}

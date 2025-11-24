package dev.goquick.kmposable.core.nav

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select

/**
 * Pushes an entry and awaits a typed result from its node. If the entry leaves the stack before
 * emitting a result, [KmposableResult.Canceled] is returned.
 *
 * Throws [IllegalArgumentException] if the node does not implement [ResultNode] for [RESULT].
 */
suspend inline fun <
    OUT : Any,
    ENTRY : KmposableStackEntry<OUT>,
    reified RESULT : Any
> KmposableNavigator<OUT, ENTRY>.pushForResult(
    noinline entryFactory: () -> ENTRY
): KmposableResult<RESULT> = coroutineScope {
    val entry = entryFactory()
    val node = entry.node
    val resultNode = node as? ResultNode<*>
        ?: throw IllegalArgumentException(
            "pushForResult requires node to implement ResultNode<${RESULT::class.simpleName}>"
        )
    @Suppress("UNCHECKED_CAST")
    val typedResultNode = resultNode as ResultNode<RESULT>

    push(entry)

    val resultDeferred = async {
        typedResultNode.result.first()
    }
    val removalDeferred = async {
        state.first { navState -> navState.stack.none { it === entry } }
        KmposableResult.Canceled
    }

    val outcome = select<KmposableResult<RESULT>> {
        resultDeferred.onAwait { it }
        removalDeferred.onAwait { it }
    }

    // Clean up the stack if the entry is still present.
    val currentStack = state.value.stack
    if (currentStack.any { it === entry }) {
        if (currentStack.last() === entry) {
            pop()
        } else {
            popTo(entry, inclusive = true)
        }
    }

    if (resultDeferred.isActive) resultDeferred.cancelAndJoin()
    if (removalDeferred.isActive) removalDeferred.cancelAndJoin()

    outcome
}

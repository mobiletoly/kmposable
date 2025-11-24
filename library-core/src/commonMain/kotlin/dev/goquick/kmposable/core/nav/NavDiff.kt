package dev.goquick.kmposable.core.nav

/**
 * Structural diff between two navigation states. Useful for hosts that need to distinguish
 * push vs pop animations without manually inspecting stacks.
 */
data class NavDiff<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val previous: KmposableNavState<OUT, ENTRY>?,
    val current: KmposableNavState<OUT, ENTRY>,
    /** Entries removed from the top, in pop order (top to bottom). */
    val popped: List<ENTRY>,
    /** Entries added to the top, in push order (bottom to top). */
    val pushed: List<ENTRY>
) {
    /** True when there is no structural change between previous and current. */
    val isNoOp: Boolean get() = popped.isEmpty() && pushed.isEmpty()
}

/**
 * Computes the structural difference between [previous] and [current].
 *
 * - If [previous] is null, the entire current stack is treated as pushed.
 * - Comparison uses reference equality on entries (consistent with unique node instances per push).
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> diffNavState(
    previous: KmposableNavState<OUT, ENTRY>?,
    current: KmposableNavState<OUT, ENTRY>
): NavDiff<OUT, ENTRY> {
    if (previous == null) {
        return NavDiff(
            previous = null,
            current = current,
            popped = emptyList(),
            pushed = current.stack
        )
    }

    val prevStack = previous.stack
    val currStack = current.stack
    val minSize = minOf(prevStack.size, currStack.size)
    var prefixLength = 0
    while (prefixLength < minSize && prevStack[prefixLength] === currStack[prefixLength]) {
        prefixLength++
    }

    val popped = prevStack.subList(prefixLength, prevStack.size).asReversed()
    val pushed = currStack.subList(prefixLength, currStack.size)

    return NavDiff(
        previous = previous,
        current = current,
        popped = popped,
        pushed = pushed
    )
}

package dev.goquick.kmposable.core.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Maintains the navigation stack and exposes immutable [KmposableNavState] updates. Implementations must
 * guarantee that the stack is never empty – every runtime always has a root entry.
 */
interface KmposableNavigator<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    /** Stream of stack snapshots; renderers observe this to update UI. */
    val state: StateFlow<KmposableNavState<OUT, ENTRY>>

    /** Adds [entry] to the top of the stack. */
    fun push(entry: ENTRY)

    /** Removes the top-most entry if more than one exists and returns it. */
    fun pop(): ENTRY?

    /** Removes all entries except the root, returning the removed entries from top to bottom. */
    fun popAll(): List<ENTRY>

    /** Replaces the entire stack with [entry] as the single element, returning the previous entries. */
    fun replaceAll(entry: ENTRY): List<ENTRY>

    /** Replaces the top-most entry and returns the removed entry, if any. */
    fun replaceTop(entry: ENTRY): ENTRY?

    /** Pops entries down to [target], optionally removing [target] when inclusive. */
    fun popTo(target: ENTRY, inclusive: Boolean = false): List<ENTRY>
}

/** Basic navigator implementation backed by an in-memory mutable list. */
class KmposableStackNavigator<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    initialEntry: ENTRY
) : KmposableNavigator<OUT, ENTRY> {

    private val stack = mutableListOf(initialEntry)
    private val _state = MutableStateFlow(KmposableNavState(stack.toList()))
    override val state: StateFlow<KmposableNavState<OUT, ENTRY>> = _state

    private fun notifyState() {
        _state.value = KmposableNavState(stack.toList())
    }

    override fun push(entry: ENTRY) {
        stack.add(entry)
        notifyState()
    }

    override fun pop(): ENTRY? {
        if (stack.size <= 1) return null
        val removed = stack.removeAt(stack.lastIndex)
        notifyState()
        return removed
    }

    override fun popAll(): List<ENTRY> {
        if (stack.size <= 1) return emptyList()
        val removed = stack.subList(1, stack.size).toList().asReversed()
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
        notifyState()
        return removed
    }

    override fun replaceAll(entry: ENTRY): List<ENTRY> {
        val removed = stack.toList().asReversed()
        stack.clear()
        stack.add(entry)
        notifyState()
        return removed
    }

    override fun replaceTop(entry: ENTRY): ENTRY? {
        check(stack.isNotEmpty()) { "Navigator stack cannot be empty" }
        val removed = if (stack.isEmpty()) null else stack.removeAt(stack.lastIndex)
        stack.add(entry)
        notifyState()
        return removed
    }

    override fun popTo(target: ENTRY, inclusive: Boolean): List<ENTRY> {
        val index = stack.indexOf(target)
        if (index == -1) return emptyList()
        val stopIndex = if (inclusive) index else index + 1
        if (stopIndex >= stack.size) return emptyList()
        val removed = mutableListOf<ENTRY>()
        while (stack.size > stopIndex) {
            removed.add(stack.removeAt(stack.lastIndex))
        }
        if (stack.isEmpty()) {
            throw IllegalStateException("Navigator stack cannot be empty")
        }
        notifyState()
        return removed
    }
}

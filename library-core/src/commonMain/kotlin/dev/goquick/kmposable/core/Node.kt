package dev.goquick.kmposable.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fundamental unit of the headless runtime. A [Node] exposes immutable state that a renderer can
 * observe, accepts UI-driven events, and emits outputs for the parent/runtime to react to
 * (typically navigation).
 *
 * Node instances are treated as unique; every push onto the stack should create a fresh instance
 * rather than reusing an old one. The runtime keys lifecycle hooks and output collectors off the
 * node reference, so reusing instances can lead to missed attach/detach calls.
 */
interface Node<STATE : Any, EVENT : Any, OUTPUT : Any> {
    val state: StateFlow<STATE>
    fun onEvent(event: EVENT)
    val outputs: Flow<OUTPUT>
}

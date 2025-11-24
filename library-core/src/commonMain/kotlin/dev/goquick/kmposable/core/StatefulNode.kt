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
package dev.goquick.kmposable.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Convenience base class that wires [Node] boilerplate: mutable state, outputs, and a child scope
 * tied to the node lifecycle. Subclasses focus on state transitions and event handling.
 *
 * @param parentScope Scope provided by the runtime; failures inside the node use a child
 * [SupervisorJob] so the rest of the app can continue running.
 * @param initialState Initial value exposed through [state].
 * @param id Optional debug identifier propagated into nav stack entries (useful in tests/logs).
 * @param outputBufferSize Extra capacity for [outputs]. Emitting more outputs than this buffer
 * allows will suspend [emitOutput] or make [tryEmitOutput] return false.
 */
abstract class StatefulNode<STATE : Any, EVENT : Any, OUTPUT : Any>(
    parentScope: CoroutineScope,
    initialState: STATE,
    val id: String? = null,
    outputBufferSize: Int = 1
) : Node<STATE, EVENT, OUTPUT>, LifecycleAwareNode {

    /** Job that ties the node's child scope to the parent app scope. */
    private val nodeJob: Job = SupervisorJob(parentScope.coroutineContext[Job])
    /** Scope used by subclasses for async work; cancelled automatically on [onDetach]. */
    protected val scope: CoroutineScope = CoroutineScope(parentScope.coroutineContext + nodeJob)

    private val _state = MutableStateFlow(initialState)
    final override val state: StateFlow<STATE> = _state.asStateFlow()

    private val _outputs = MutableSharedFlow<OUTPUT>(extraBufferCapacity = outputBufferSize)
    final override val outputs = _outputs.asSharedFlow()

    /** Replaces the current state, notifying observers. */
    protected fun setState(value: STATE) {
        _state.value = value
    }

    /** Applies a reducer to produce a new state based on the previous value. */
    protected fun updateState(reducer: (STATE) -> STATE) {
        _state.update(reducer)
    }

    /** Suspends until the output is delivered to collectors. */
    protected suspend fun emitOutput(output: OUTPUT) {
        runCatching { _outputs.emit(output) }.onFailure(::handleException)
    }

    /** Non-suspending variant for fire-and-forget outputs. */
    protected fun tryEmitOutput(output: OUTPUT): Boolean =
        runCatching { _outputs.tryEmit(output) }.onFailure(::handleException).getOrDefault(false)

    override fun onDetach() {
        onCleared()
        nodeJob.cancel()
    }

    /**
     * Override for custom cleanup logic triggered right before the node's scope is cancelled.
     * Any coroutines launched in [scope] should complete quickly because the enclosing
     * [nodeJob] is cancelled immediately after this callback returns.
     */
    protected open fun onCleared() {}

    /** Hook for surfacing unexpected exceptions (logging, error state, etc.). */
    protected open fun handleException(error: Throwable) {}
}

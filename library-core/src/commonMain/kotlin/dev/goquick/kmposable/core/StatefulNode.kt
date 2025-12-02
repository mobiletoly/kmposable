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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
 * @param errorLogger Optional hook for surfacing unexpected exceptions.
 */
abstract class StatefulNode<STATE : Any, EVENT : Any, OUTPUT : Any>(
    parentScope: CoroutineScope,
    initialState: STATE,
    val id: String? = null,
    outputBufferSize: Int = 1,
    private val errorLogger: dev.goquick.kmposable.core.logging.NodeErrorLogger? = null
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
    protected open fun handleException(error: Throwable) {
        errorLogger?.onNodeError(this, error)
    }

    /**
     * Convenience wrapper around [runCatching] that applies state reducers for start/success/failure.
     *
     * @param onStart reducer applied before invoking [block] (e.g., set loading = true).
     * @param onEach reducer applied when [block] succeeds with a value.
     * @param onError reducer applied when [block] throws.
     */
    protected suspend fun <R> runCatchingState(
        onStart: (STATE) -> STATE = { it },
        onEach: (STATE, R) -> STATE,
        onError: (STATE, Throwable) -> STATE = { state, _ -> state },
        block: suspend () -> R
    ): Result<R> {
        updateState(onStart)
        val result = runCatching { block() }
        result.fold(
            onSuccess = { value -> updateState { current -> onEach(current, value) } },
            onFailure = { error -> updateState { current -> onError(current, error) } }
        )
        return result
    }

    /**
     * Mirrors a child [StateFlow] into this node's state by applying [map] whenever the child emits.
     * Collection runs on the node scope and starts immediately.
     */
    fun <CHILD_STATE : Any> mirrorChildState(
        childState: StateFlow<CHILD_STATE>,
        map: (STATE, CHILD_STATE) -> STATE
    ): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        childState.collect { child ->
            updateState { parent -> map(parent, child) }
        }
    }
}

package dev.goquick.kmposable.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Result emitted by a result-producing node. */
sealed interface KmposableResult<out R> {
    data class Ok<R>(val value: R) : KmposableResult<R>
    data object Canceled : KmposableResult<Nothing>
}

/** Contract for nodes that can produce a single typed result independent of their outputs. */
interface ResultNode<RESULT : Any> {
    val result: Flow<KmposableResult<RESULT>>
}

/**
 * Convenience base node that wires up a [MutableSharedFlow] for results.
 *
 * Subclasses call [emitResult]/[tryEmitResult] or the typed helpers [emitOk] / [emitCanceled]
 * to complete the result channel. This is purely additive; outputs are still available
 * via [StatefulNode] for navigation or other host signals.
 */
abstract class ResultfulStatefulNode<
    STATE : Any,
    EVENT : Any,
    OUTPUT : Any,
    RESULT : Any
>(
    parentScope: CoroutineScope,
    initialState: STATE,
    id: String? = null,
    outputBufferSize: Int = 1,
    resultBufferSize: Int = 1
) : StatefulNode<STATE, EVENT, OUTPUT>(
    parentScope = parentScope,
    initialState = initialState,
    id = id,
    outputBufferSize = outputBufferSize
), ResultNode<RESULT> {

    private val _result = MutableSharedFlow<KmposableResult<RESULT>>(
        replay = 1,
        extraBufferCapacity = resultBufferSize
    )
    override val result: Flow<KmposableResult<RESULT>> = _result.asSharedFlow()

    protected suspend fun emitResult(result: KmposableResult<RESULT>) {
        _result.emit(result)
    }

    protected fun tryEmitResult(result: KmposableResult<RESULT>): Boolean {
        return _result.tryEmit(result)
    }

    protected suspend fun emitOk(value: RESULT) {
        emitResult(KmposableResult.Ok(value))
    }

    protected fun tryEmitOk(value: RESULT): Boolean {
        return tryEmitResult(KmposableResult.Ok(value))
    }

    protected suspend fun emitCanceled() {
        emitResult(KmposableResult.Canceled)
    }

    protected fun tryEmitCanceled(): Boolean {
        return tryEmitResult(KmposableResult.Canceled)
    }
}

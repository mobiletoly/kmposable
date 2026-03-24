package dev.goquick.kmposable.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Combines [ResultfulStatefulNode] with [EffectSource] for features that need both one-off UI
 * effects and a typed result channel.
 */
abstract class EffectfulResultfulStatefulNode<
    STATE : Any,
    EVENT : Any,
    OUTPUT : Any,
    EFFECT : Any,
    RESULT : Any
>(
    parentScope: CoroutineScope,
    initialState: STATE,
    id: String? = null,
    outputBufferSize: Int = 1,
    resultBufferSize: Int = 1,
    effectBufferSize: Int = 0
) : ResultfulStatefulNode<STATE, EVENT, OUTPUT, RESULT>(
    parentScope = parentScope,
    initialState = initialState,
    id = id,
    outputBufferSize = outputBufferSize,
    resultBufferSize = resultBufferSize
), EffectSource<EFFECT> {

    private val _effects = MutableSharedFlow<EFFECT>(extraBufferCapacity = effectBufferSize)
    override val effects: SharedFlow<EFFECT> = _effects.asSharedFlow()

    /** Suspends until the effect is delivered to collectors. */
    protected suspend fun emitEffect(effect: EFFECT) {
        runCatching { _effects.emit(effect) }.onFailure(::handleException)
    }

    /** Non-suspending variant for fire-and-forget effects. */
    protected fun tryEmitEffect(effect: EFFECT): Boolean =
        runCatching { _effects.tryEmit(effect) }.onFailure(::handleException).getOrDefault(false)
}

/**
 * Convenience base for result-only nodes that also expose one-off UI effects.
 */
typealias EffectfulResultOnlyNode<STATE, EVENT, EFFECT, RESULT> =
    EffectfulResultfulStatefulNode<STATE, EVENT, Nothing, EFFECT, RESULT>

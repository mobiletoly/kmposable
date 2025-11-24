package dev.goquick.kmposable.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Opt-in contract for nodes that emit one-off side effects (analytics, toasts, etc.). */
interface EffectSource<EFFECT : Any> {
    val effects: SharedFlow<EFFECT>
}

/** Host-facing handler for side effects. */
fun interface EffectHandler<EFFECT : Any> {
    suspend fun handle(effect: EFFECT)
}

/**
 * Convenience base class that combines [StatefulNode] with an effects stream. Subclasses can emit
 * transient effects without overloading navigation outputs.
 */
abstract class EffectfulStatefulNode<
    STATE : Any,
    EVENT : Any,
    OUTPUT : Any,
    EFFECT : Any
    >(
    parentScope: CoroutineScope,
    initialState: STATE,
    id: String? = null,
    outputBufferSize: Int = 1,
    effectBufferSize: Int = 0
) : StatefulNode<STATE, EVENT, OUTPUT>(
    parentScope = parentScope,
    initialState = initialState,
    id = id,
    outputBufferSize = outputBufferSize
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

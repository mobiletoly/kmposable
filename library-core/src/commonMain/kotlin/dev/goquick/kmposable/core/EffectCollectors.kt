package dev.goquick.kmposable.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Collects one-off [EffectSource.effects] on the provided [CoroutineScope] and forwards each
 * emission to [handler]. The returned [Job] can be cancelled to stop collection.
 */
fun <EFFECT : Any> CoroutineScope.collectEffects(
    source: EffectSource<EFFECT>,
    handler: suspend (EFFECT) -> Unit
): Job = launch(start = CoroutineStart.UNDISPATCHED) {
    source.effects.collect { effect ->
        handler(effect)
    }
}

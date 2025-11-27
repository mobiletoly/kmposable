package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import dev.goquick.kmposable.core.EffectSource
import dev.goquick.kmposable.core.collectEffects

/**
 * Convenience composable that collects [EffectSource.effects] for the given [source] and invokes
 * [onEffect] for each emission. Collection is tied to the surrounding composable lifecycle.
 */
@Composable
fun <EFFECT : Any> CollectEffects(
    source: EffectSource<EFFECT>,
    onEffect: suspend (EFFECT) -> Unit
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(source, onEffect) {
        val job = scope.collectEffects(source, onEffect)
        onDispose { job.cancel() }
    }
}

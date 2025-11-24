package dev.goquick.kmposable.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

private data class EffectEvent(val message: String)

private class EffectNode(
    scope: kotlinx.coroutines.CoroutineScope,
    effectBufferSize: Int
) : EffectfulStatefulNode<Unit, Unit, Unit, EffectEvent>(
    parentScope = scope,
    initialState = Unit,
    effectBufferSize = effectBufferSize
) {
    override fun onEvent(event: Unit) = Unit
    suspend fun emit(effect: EffectEvent) = emitEffect(effect)
    fun tryEmit(effect: EffectEvent): Boolean = tryEmitEffect(effect)
}

@OptIn(ExperimentalCoroutinesApi::class)
class EffectsTest {

    @Test
    fun emitsEffectsToCollectors() = runTest {
        val node = EffectNode(this, effectBufferSize = 0)
        val received = mutableListOf<EffectEvent>()
        val job = launch { node.effects.collect { received += it } }
        advanceUntilIdle() // ensure collector is active

        node.emit(EffectEvent("hello"))
        advanceUntilIdle()

        assertEquals(listOf(EffectEvent("hello")), received)
        job.cancelAndJoin()
        node.onDetach()
    }

    @Test
    fun effectsDropWithoutCollectorsAndDeliverWhenCollecting() = runTest {
        val node = EffectNode(this, effectBufferSize = 0)
        val received = mutableListOf<EffectEvent>()
        val first = node.tryEmit(EffectEvent("dropped"))
        advanceUntilIdle()
        assertTrue(first)
        assertEquals(emptyList<EffectEvent>(), received) // no replay without collectors

        val job = launch { node.effects.collect { received += it } }
        advanceUntilIdle()

        node.emit(EffectEvent("delivered"))
        advanceUntilIdle()

        job.cancelAndJoin()

        assertEquals(listOf(EffectEvent("delivered")), received)
        node.onDetach()
    }
}

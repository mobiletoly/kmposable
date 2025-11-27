package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.EffectSource
import dev.goquick.kmposable.core.collectEffects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

class EffectCollectorsTest {

    @Test
    fun collects_effects_and_can_be_cancelled() = runBlocking(Dispatchers.Unconfined) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val source = EffectStub()
        val received = mutableListOf<String>()

        val job = scope.collectEffects(source) { received += it }
        source.emit("one")
        source.emit("two")

        withTimeout(200) {
            while (received.size < 2) {
                yield()
            }
        }
        assertEquals(listOf("one", "two"), received)

        job.cancel()
        val sizeAfterCancel = received.size
        source.emit("ignored")
        assertEquals(sizeAfterCancel, received.size) // no new items after cancel

        scope.cancel()
        assertTrue(true) // reached end without hangs
    }

    private class EffectStub : EffectSource<String> {
        private val flow = MutableSharedFlow<String>(extraBufferCapacity = 4)
        override val effects = flow
        fun emit(value: String) {
            flow.tryEmit(value)
        }
    }
}

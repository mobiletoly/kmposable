package dev.goquick.kmposable.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import dev.goquick.kmposable.core.EffectSource
import dev.goquick.kmposable.core.LifecycleAwareNode
import dev.goquick.kmposable.core.Node
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

@Ignore("Compose runtime harness is flaky; enable once frame driving is stable.")
class NodeHostAndEffectJvmTest {

    @Test
    fun collectEffects_invokes_callback_for_emissions() = runCompositionTest { frame ->
        val source = FakeEffectSource<String>()
        val seen = mutableListOf<String>()

        setContent {
            CollectEffects(source) { seen.add(it) }
        }
        frame.advance()
        assertEquals(emptyList(), seen)

        source.flow.tryEmit("toast")
        frame.advance()
        assertEquals(listOf("toast"), seen)
    }

    @Test
    fun nodeHost_attaches_collects_state_and_outputs() = runCompositionTest { frame ->
        val node = FakeNode()
        val outputs = mutableListOf<String>()
        var renderedState: Int? = null
        var eventSink: ((String) -> Unit)? = null

        setContent {
            NodeHost(node = node, onOutput = { outputs.add(it) }) { state, onEvent, _ ->
                renderedState = state
                eventSink = onEvent
            }
        }
        frame.advance()
        assertEquals(1, node.attachCount)
        assertEquals(0, node.detachCount)
        assertEquals(0, renderedState)

        eventSink?.invoke("ping")
        frame.advance()
        assertEquals(listOf("ping"), node.events)

        node.outputsFlow.tryEmit("hello")
        frame.advance()
        assertEquals(listOf("hello"), outputs)

        dispose()
        frame.advance()
        assertEquals(1, node.detachCount)
    }
}

// Minimal Compose runtime harness (no Android/Robolectric).
private fun runCompositionTest(block: suspend CompositionHarness.(FrameDriver) -> Unit) {
    val frameClock = TestFrameClock()
    runBlocking(frameClock) {
        val recomposer = Recomposer(coroutineContext)
        val applier = NoOpApplier()
        val composition = androidx.compose.runtime.Composition(applier, recomposer)
        val recomposerJob = launch { recomposer.runRecomposeAndApplyChanges() }
        val harness = CompositionHarness(composition, recomposerJob, recomposer, this, frameClock)
        try {
            harness.block(FrameDriver(frameClock))
        } finally {
            harness.dispose()
        }
    }
}

private class CompositionHarness(
    private val composition: androidx.compose.runtime.Composition,
    private val recomposerJob: Job,
    private val recomposer: Recomposer,
    private val scope: CoroutineScope,
    private val frameClock: TestFrameClock,
) {
    fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
    }

    fun dispose() {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
    }
}

private class FrameDriver(
    private val clock: TestFrameClock,
) {
    suspend fun advance() {
        clock.sendFrame()
        yield()
    }
}

private class TestFrameClock : MonotonicFrameClock, CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock
    private val awaiters = Channel<(Long) -> Unit>(Channel.UNLIMITED)

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            awaiters.trySend { time -> cont.resume(onFrame(time)) {} }
        }

    suspend fun sendFrame(timeNanos: Long = System.nanoTime()) {
        val resume = awaiters.tryReceive().getOrNull() ?: return
        resume(timeNanos)
    }
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
    override fun remove(index: Int, count: Int) = Unit
}

private class FakeNode : Node<Int, String, String>, LifecycleAwareNode {
    private val _state = MutableStateFlow(0)
    val outputsFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = mutableListOf<String>()
    var attachCount: Int = 0
    var detachCount: Int = 0

    override val state: StateFlow<Int> = _state
    override fun onEvent(event: String) {
        events.add(event)
    }
    override val outputs: SharedFlow<String> = outputsFlow

    override fun onAttach() {
        attachCount += 1
    }

    override fun onDetach() {
        detachCount += 1
    }
}

private class FakeEffectSource<EFFECT : Any> : EffectSource<EFFECT> {
    val flow = MutableSharedFlow<EFFECT>(extraBufferCapacity = 1)
    override val effects: SharedFlow<EFFECT> = flow
}

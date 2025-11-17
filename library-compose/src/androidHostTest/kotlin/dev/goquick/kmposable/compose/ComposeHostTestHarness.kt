package dev.goquick.kmposable.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

internal class ComposeHostTestScope(
    private val frameClock: BroadcastFrameClock,
    private val composition: Composition,
    private val recomposerJob: Job,
    private val scope: CoroutineScope
) {
    suspend fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
        awaitIdle()
    }

    suspend fun awaitIdle() {
        frameClock.sendFrame(0L)
        yield()
        Snapshot.sendApplyNotifications()
    }

    suspend fun dispose() {
        try {
            composition.dispose()
        } catch (failure: RuntimeException) {
            val message = failure.message.orEmpty()
            if (!message.contains("android.os.Trace")) throw failure
        }
        recomposerJob.cancelAndJoin()
        scope.cancel()
    }
}

internal suspend fun withComposeHostTest(
    block: suspend ComposeHostTestScope.() -> Unit
) {
    val frameClock = BroadcastFrameClock()
    val recomposerContext = frameClock + Dispatchers.Unconfined
    val scope = CoroutineScope(SupervisorJob() + recomposerContext)
    val recomposer = Recomposer(recomposerContext)
    val composition = Composition(NoOpApplier(), recomposer)
    val job = scope.launch {
        recomposer.runRecomposeAndApplyChanges()
    }
    val hostScope = ComposeHostTestScope(frameClock, composition, job, scope)
    try {
        hostScope.block()
    } finally {
        hostScope.dispose()
    }
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) {}
    override fun insertBottomUp(index: Int, instance: Unit) {}
    override fun move(from: Int, to: Int, count: Int) {}
    override fun remove(index: Int, count: Int) {}
    override fun onClear() {}
}

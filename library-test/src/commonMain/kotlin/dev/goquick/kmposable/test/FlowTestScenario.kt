package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.runtime.NavFlowFactory
import dev.goquick.kmposable.runtime.NavFlow
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Test harness that drives a [NavFlow] without rendering UI. Tests can feed events,
 * inspect stack metadata, and assert emitted outputs via this fluent DSL.
 */
class FlowTestScenario<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val runtime: NavFlow<OUT, ENTRY>,
    private val scope: CoroutineScope
) {
    private val collectedOutputs = ArrayDeque<OUT>()
    private var outputsJob: Job? = null
    private var started = false

    suspend fun start(): FlowTestScenario<OUT, ENTRY> = apply {
        if (started) return@apply
        started = true
        outputsJob = scope.launch {
            runtime.outputs.collect { collectedOutputs.addLast(it) }
        }
        runtime.start()
    }

    fun send(event: Any): FlowTestScenario<OUT, ENTRY> = apply {
        runtime.sendEvent(event)
    }

    fun assertCanPop(expected: Boolean): FlowTestScenario<OUT, ENTRY> = apply {
        val actual = runtime.canPop()
        check(actual == expected) {
            "Expected canPop=$expected but was ${runtime.canPop()}"
        }
    }

    fun pop(): FlowTestScenario<OUT, ENTRY> = apply {
        val popped = runtime.pop()
        check(popped) { "Expected pop() to succeed but canPop() was false." }
    }

    fun popIfPossible(): FlowTestScenario<OUT, ENTRY> = apply {
        runtime.pop()
    }

    fun assertTopNodeTag(expectedTag: String?): FlowTestScenario<OUT, ENTRY> = apply {
        val navState = runtime.navState.value
        val actual = navState.stack.lastOrNull()?.tag
            ?: error("Expected top tag '$expectedTag' but navigation stack is empty.")
        check(actual == expectedTag) {
            "Expected top tag '$expectedTag' but was '$actual'"
        }
    }

    fun assertNextOutput(expected: OUT): FlowTestScenario<OUT, ENTRY> = apply {
        val actual = if (collectedOutputs.isEmpty()) {
            null
        } else {
            collectedOutputs.removeFirst()
        } ?: error(
            "Expected next output '$expected' but collector queue was empty.\n" +
                    "Buffered outputs: $collectedOutputs"
        )
        check(actual == expected) {
            "Expected next output '$expected' but was '$actual'"
        }
    }

    fun assertNoMoreOutputs(): FlowTestScenario<OUT, ENTRY> = apply {
        check(collectedOutputs.isEmpty()) {
            "Expected no more outputs, but found: $collectedOutputs"
        }
    }

    fun assertStackSize(expected: Int): FlowTestScenario<OUT, ENTRY> = apply {
        val size = runtime.navState.value.stack.size
        check(size == expected) {
            "Expected stack size $expected but was $size"
        }
    }

    fun assertStackTags(vararg expectedTags: String?): FlowTestScenario<OUT, ENTRY> = apply {
        val actualTags = runtime.navState.value.stack.map { it.tag }
        check(actualTags == expectedTags.toList()) {
            "Expected stack tags ${expectedTags.toList()} but was $actualTags"
        }
    }

    inline fun <reified T : Node<*, *, OUT>> assertTopNodeIs(): FlowTestScenario<OUT, ENTRY> = apply {
        val topNode = runtime.navState.value.top
        check(topNode is T) {
            "Expected top node of type ${T::class.simpleName}, but was ${topNode::class.simpleName}"
        }
    }

    suspend fun awaitNextOutput(timeoutMillis: Long = 1_000): OUT {
        val startTime = System.currentTimeMillis()
        while (collectedOutputs.isEmpty()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                error("Timed out waiting for next output. Buffered: $collectedOutputs")
            }
            // This is crude; in real tests you'd likely use a TestCoroutineScheduler advance.
            kotlinx.coroutines.yield()
        }
        return collectedOutputs.removeFirst()
    }

    suspend fun finish() {
        try {
            outputsJob?.cancelAndJoin()
        } catch (_: CancellationException) {
        }
        runtime.dispose()
    }
}

/**
 * Creates a [FlowTestScenario] from a [NavFlowFactory] for convenient use in tests.
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlowFactory<OUT, ENTRY>.createTestScenario(
    scope: CoroutineScope
): FlowTestScenario<OUT, ENTRY> = FlowTestScenario(createNavFlow(), scope)

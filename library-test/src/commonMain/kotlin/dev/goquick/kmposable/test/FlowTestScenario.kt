/*
 * Copyright 2025 Toly Pochkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.goquick.kmposable.test

import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.NavFlowFactory
import dev.goquick.kmposable.runtime.NavFlowScriptScope
import dev.goquick.kmposable.runtime.launchNavFlowScript
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Test harness that drives a [NavFlow] without rendering UI.
 *
 * Use it to start the runtime, send events, assert navigation state and
 * observe outputs while keeping everything headless and deterministic.
 */
class FlowTestScenario<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val navFlow: NavFlow<OUT, ENTRY>,
    private val scope: CoroutineScope
) {
    @Deprecated("Use navFlow", ReplaceWith("navFlow"))
    val runtime: NavFlow<OUT, ENTRY> get() = navFlow
    private val collectedOutputs = ArrayDeque<OUT>()
    private var outputsJob: Job? = null
    @PublishedApi
    internal var started = false

    /** Starts collecting outputs and starts the underlying [NavFlow] once. */
    suspend fun start(): FlowTestScenario<OUT, ENTRY> = apply {
        if (started) return@apply
        started = true
        outputsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            navFlow.outputs.collect { collectedOutputs.addLast(it) }
        }
        navFlow.start()
    }

    /** Injects an [event] into the currently visible node. */
    fun send(event: Any): FlowTestScenario<OUT, ENTRY> = apply {
        navFlow.sendEvent(event)
    }

    /** Verifies whether the stack can pop. */
    fun assertCanPop(expected: Boolean): FlowTestScenario<OUT, ENTRY> = apply {
        val actual = navFlow.canPop()
        check(actual == expected) {
            "Expected canPop=$expected but was ${navFlow.canPop()}"
        }
    }

    /** Pops the stack and fails if already at root. */
    fun pop(): FlowTestScenario<OUT, ENTRY> = apply {
        val popped = navFlow.pop()
        check(popped) { "Expected pop() to succeed but canPop() was false." }
    }

    /** Attempts to pop but ignores the result, useful for cleanup. */
    fun popIfPossible(): FlowTestScenario<OUT, ENTRY> = apply {
        navFlow.pop()
    }

    /** Verifies the current top entry tag. */
    fun assertTopNodeTag(expectedTag: String?): FlowTestScenario<OUT, ENTRY> = apply {
        val navState = navFlow.navState.value
        val actual = navState.stack.lastOrNull()?.tag
            ?: error("Expected top tag '$expectedTag' but navigation stack is empty.")
        check(actual == expectedTag) {
            "Expected top tag '$expectedTag' but was '$actual'"
        }
    }

    /** Suspends until the next output arrives and asserts it equals [expected]. */
    suspend fun assertNextOutput(expected: OUT): FlowTestScenario<OUT, ENTRY> = apply {
        val actual = awaitNextOutput()
        check(actual == expected) {
            "Expected next output '$expected' but was '$actual'"
        }
    }

    /** Ensures output buffer is empty. */
    fun assertNoMoreOutputs(): FlowTestScenario<OUT, ENTRY> = apply {
        check(collectedOutputs.isEmpty()) {
            "Expected no more outputs, but found: $collectedOutputs"
        }
    }

    /** Verifies stack size synchronously. */
    fun assertStackSize(expected: Int): FlowTestScenario<OUT, ENTRY> = apply {
        val size = navFlow.navState.value.stack.size
        check(size == expected) {
            "Expected stack size $expected but was $size"
        }
    }

    /** Verifies stack tags synchronously. */
    fun assertStackTags(vararg expectedTags: String?): FlowTestScenario<OUT, ENTRY> = apply {
        val actualTags = navFlow.navState.value.stack.map { it.tag }
        check(actualTags == expectedTags.toList()) {
            "Expected stack tags ${expectedTags.toList()} but was $actualTags"
        }
    }

    /** Suspends until the stack reaches [expected] size or times out. */
    suspend fun awaitStackSize(expected: Int, timeoutMillis: Long = 1_000): FlowTestScenario<OUT, ENTRY> = apply {
        check(started) { "Call start() before awaiting navigation changes." }
        if (navFlow.navState.value.stack.size == expected) return@apply
        withTimeout(timeoutMillis) {
            navFlow.navState
                .filter { it.stack.size == expected }
                .first()
        }
    }

    /** Suspends until the stack tags equal [expectedTags] or times out. */
    suspend fun awaitStackTags(vararg expectedTags: String?, timeoutMillis: Long = 1_000): FlowTestScenario<OUT, ENTRY> = apply {
        check(started) { "Call start() before awaiting navigation changes." }
        val expectedList = expectedTags.toList()
        if (navFlow.navState.value.stack.map { it.tag } == expectedList) return@apply
        withTimeout(timeoutMillis) {
            navFlow.navState
                .filter { state -> state.stack.map { it.tag } == expectedList }
                .first()
        }
    }

    /** Verifies that the current top node is of type [T]. */
    inline fun <reified T : Node<*, *, OUT>> assertTopNodeIs(): FlowTestScenario<OUT, ENTRY> = apply {
        val topNode = navFlow.navState.value.top
        check(topNode is T) {
            "Expected top node of type ${T::class.simpleName}, but was ${topNode::class.simpleName}"
        }
    }

    /** Suspends until the top node is of type [T], or times out. */
    suspend inline fun <reified T : Node<*, *, OUT>> awaitTopNodeIs(
        timeoutMillis: Long = 1_000
    ): FlowTestScenario<OUT, ENTRY> = apply {
        check(started) { "Call start() before awaiting navigation changes." }
        if (navFlow.navState.value.top is T) return@apply
        withTimeout(timeoutMillis) {
            navFlow.navState
                .filter { it.top is T }
                .first()
        }
    }

    /** Awaits the next output and returns it (throws on timeout). */
    suspend fun awaitNextOutput(timeoutMillis: Long = 1_000): OUT {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (collectedOutputs.isEmpty()) {
            if (System.currentTimeMillis() > deadline) {
                error("Timed out waiting for next output. Buffered: $collectedOutputs")
            }
            kotlinx.coroutines.yield()
        }
        return collectedOutputs.removeFirst()
    }

    /** Stops collecting outputs and disposes the runtime. */
    suspend fun finish() {
        try {
            outputsJob?.cancelAndJoin()
        } catch (_: CancellationException) {
        }
        navFlow.dispose()
    }

    /** Launches the provided [script] against the same NavFlow the UI would use. */
    fun launchScript(
        onTrace: ((String) -> Unit)? = null,
        script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
    ): Job = navFlow.launchNavFlowScript(scope, onTrace, script)

    /** Suspends until [mapper] produces a value from the next output and returns it. */
    suspend fun <T : Any> awaitMappedOutput(
        timeoutMillis: Long = 1_000,
        mapper: (OUT) -> T?
    ): T {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (true) {
            val next = awaitNextOutput(timeoutMillis)
            mapper(next)?.let { return it }
            if (System.currentTimeMillis() > deadline) {
                error("Timed out waiting for mapped output. Buffered: $collectedOutputs")
            }
        }
    }

    /** Suspends until an output of type [T] is emitted and returns it. */
    suspend inline fun <reified T : OUT> awaitOutputOfType(
        timeoutMillis: Long = 1_000
    ): T = awaitMappedOutput(timeoutMillis) { output -> output as? T }

    /**
     * Awaits a typed result from a [ResultNode] currently on top of the stack. If the node leaves
     * the stack before producing a result, returns [KmposableResult.Canceled].
     */
    suspend inline fun <reified RESULT : Any> awaitTopResult(
        timeoutMillis: Long = 1_000
    ): KmposableResult<RESULT> {
        val top = navFlow.currentTopNode()
        val resultNode = top as? ResultNode<RESULT>
            ?: error("Top node is not a ResultNode<${RESULT::class.simpleName}>")

        return withTimeout(timeoutMillis) {
            channelFlow {
                val resultJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    resultNode.result.collect { send(it) }
                }
                val removalJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    navFlow.navState
                        .filter { state -> state.stack.none { it.node == top } }
                        .first()
                    send(KmposableResult.Canceled)
                }
                awaitClose {
                    resultJob.cancel()
                    removalJob.cancel()
                }
            }.first()
        }
    }
}

/** Creates a [FlowTestScenario] from a [NavFlowFactory] for convenient use in tests. */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlowFactory<OUT, ENTRY>.createTestScenario(
    scope: CoroutineScope
): FlowTestScenario<OUT, ENTRY> = FlowTestScenario(createNavFlow(), scope)

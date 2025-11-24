package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.StatefulNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest

private sealed interface TestOutput {
    data class Submit(val payload: String) : TestOutput
    data class Done(val code: Int) : TestOutput
    data class Cancelled(val reason: String) : TestOutput
}

private class TestNode(scope: CoroutineScope) :
    StatefulNode<Unit, Any, TestOutput>(scope, Unit) {
    override fun onEvent(event: Any) = Unit
    fun emit(output: TestOutput) {
        check(tryEmitOutput(output)) { "Failed to emit $output" }
    }
}

class FlowScriptDslTest {

    @Test
    fun stepsRunSequentiallyAndFinishSkipsRest() = runTest {
        val node = TestNode(this)
        val navFlow = NavFlow(this, node)
        navFlow.start()

        val observed = mutableListOf<String>()
        navFlow.runFlow(this) {
            step("first") { observed += "first" }
            step("second") {
                observed += "second"
                finish()
            }
            step("third") { observed += "third" }
        }.join()
        navFlow.dispose()
        testScheduler.runCurrent()

        assertEquals(listOf("first", "second"), observed)
    }

    @Test
    fun awaitOutputAndBranchHandleOutputs() = runTest {
        val node = TestNode(this)
        val navFlow = NavFlow(this, node)
        navFlow.start()

        val handled = mutableListOf<String>()
        val scriptJob = navFlow.runFlow(this) {
            step("await submit") {
                awaitOutput<TestOutput.Submit> { handled += "submit:${it.payload}" }
            }
            step("branch result") {
                branch {
                    on<TestOutput.Done> { handled += "done:${it.code}" }
                    on<TestOutput.Cancelled> { handled += "cancel:${it.reason}" }
                }
            }
        }

        node.emit(TestOutput.Submit("hello"))
        testScheduler.runCurrent()
        node.emit(TestOutput.Done(200))

        testScheduler.runCurrent()
        scriptJob.join()
        navFlow.dispose()
        testScheduler.runCurrent()

        assertEquals(listOf("submit:hello", "done:200"), handled)
    }

    @Test
    fun callRoutesSuccessAndFailureHandlers() = runTest {
        val node = TestNode(this)
        val navFlow = NavFlow(this, node)
        navFlow.start()

        val events = mutableListOf<String>()

        navFlow.runFlow(this) {
            step("success call") {
                call { "ok" }
                    .onSuccess { events += "success:$it" }
                    .onFailure { events += "failure" }
            }
            step("failure call") {
                call<String> { error("boom") }
                    .onSuccess { events += "unexpected success" }
                    .onFailure { error -> events += "failure:${error.message}" }
            }
        }.join()
        navFlow.dispose()
        testScheduler.runCurrent()

        assertEquals(listOf("success:ok", "failure:boom"), events)
    }

    @Test
    fun cancelHandlerRunsWhenJobCancelled() = runTest {
        val node = TestNode(this)
        val navFlow = NavFlow(this, node)
        navFlow.start()

        val calls = mutableListOf<String>()
        val scriptJob = navFlow.runFlow(this) {
            cancel("test") { calls += "cancelled" }
            step("waiting") {
                awaitOutput<TestOutput.Submit> { calls += "submit" }
            }
        }

        testScheduler.runCurrent()
        scriptJob.cancel()
        scriptJob.join()
        navFlow.dispose()
        testScheduler.runCurrent()

        assertEquals(listOf("cancelled"), calls)
    }
}

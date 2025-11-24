package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.KmposableNavigator
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Runs a declarative flow script on top of this [NavFlow], compiling the DSL down to the existing
 * [NavFlowScriptScope] APIs. The flow must already be started by the caller.
 */
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlow<OUT, ENTRY>.runFlow(
    scope: CoroutineScope,
    onTrace: ((String) -> Unit)? = null,
    build: FlowScriptBuilder<OUT, ENTRY>.() -> Unit
): Job {
    val script = FlowScriptBuilder<OUT, ENTRY>().apply(build).build()
    return launchNavFlowScript(scope, onTrace) {
        FlowScriptExecutor(script).run(this)
    }
}

/** Collects steps and handlers for a declarative flow script. */
class FlowScriptBuilder<OUT : Any, ENTRY : KmposableStackEntry<OUT>> internal constructor() {
    private val steps = mutableListOf<FlowStep<OUT, ENTRY>>()
    private var cancelHandler: FlowCancelHandler<OUT, ENTRY>? = null

    fun step(name: String, block: suspend FlowStepContext<OUT, ENTRY>.() -> Unit) {
        steps += FlowStep(name, block)
    }

    fun cancel(
        reason: String? = null,
        block: suspend FlowStepContext<OUT, ENTRY>.() -> Unit
    ) {
        cancelHandler = FlowCancelHandler(reason, block)
    }

    internal fun build(): FlowScript<OUT, ENTRY> = FlowScript(steps.toList(), cancelHandler)
}

internal data class FlowStep<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val name: String,
    val block: suspend FlowStepContext<OUT, ENTRY>.() -> Unit
)

internal data class FlowCancelHandler<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val reason: String?,
    val block: suspend FlowStepContext<OUT, ENTRY>.() -> Unit
)

internal data class FlowScript<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val steps: List<FlowStep<OUT, ENTRY>>,
    val cancelHandler: FlowCancelHandler<OUT, ENTRY>?
)

internal class FinishFlag {
    var finished: Boolean = false
        private set

    fun markFinished() {
        finished = true
    }
}

class FlowStepContext<OUT : Any, ENTRY : KmposableStackEntry<OUT>> internal constructor(
    @PublishedApi internal val scriptScope: NavFlowScriptScope<OUT, ENTRY>,
    private val finishFlag: FinishFlag
) {
    val navFlow: NavFlow<OUT, ENTRY> get() = scriptScope.navFlow
    val navigator: KmposableNavigator<OUT, ENTRY> get() = scriptScope.navigator
    /** Access to the underlying script scope for interop with NavFlowScript helpers. */
    val script: NavFlowScriptScope<OUT, ENTRY> get() = scriptScope

    fun finish() {
        finishFlag.markFinished()
    }

    fun trace(message: () -> String) {
        scriptScope.trace(message)
    }

    suspend inline fun <reified T : OUT> awaitOutput(
        noinline handler: suspend (T) -> Unit
    ): T {
        val output = scriptScope.awaitOutput<T> { it is T }
        handler(output)
        return output
    }

    suspend inline fun <reified T : OUT> awaitOutputOfType(): T =
        scriptScope.awaitOutput<T> { it is T }

    suspend fun <T> call(block: suspend () -> T): FlowCallResult<T> =
        try {
            FlowCallResult(Result.success(block()))
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            FlowCallResult(Result.failure(t))
        }

    suspend inline fun <reified T : Node<*, *, OUT>> updateNode(
        noinline block: suspend T.() -> Unit
    ) {
        val node = navFlow.currentTopNode()
        val typed = node as? T
            ?: error("Expected top node of type ${T::class.simpleName}, but was ${node::class.simpleName}")
        block(typed)
    }

    suspend inline fun <reified T : Node<*, *, OUT>, R> withNode(
        noinline block: suspend T.() -> R
    ): R {
        val node = navFlow.currentTopNode()
        val typed = node as? T
            ?: error("Expected top node of type ${T::class.simpleName}, but was ${node::class.simpleName}")
        return block(typed)
    }

    suspend fun action(block: suspend () -> Unit) {
        block()
    }

    suspend fun branch(build: BranchBuilder<OUT>.() -> Unit) {
        val builder = BranchBuilder<OUT>().apply(build)
        while (true) {
            val next = scriptScope.awaitOutput<OUT> { true }
            if (builder.handle(next)) return
        }
    }

    suspend fun <R> awaitOutputCase(builder: OutputCaseBuilder<OUT, R>.() -> Unit): R {
        return scriptScope.awaitOutputCase(builder)
    }

    suspend fun runSubflow(build: FlowScriptBuilder<OUT, ENTRY>.() -> Unit) {
        val subScript = FlowScriptBuilder<OUT, ENTRY>().apply(build).build()
        FlowScriptExecutor(subScript, finishFlag).run(scriptScope)
    }
}

class FlowCallResult<T> internal constructor(
    private val result: Result<T>
) {
    suspend fun onSuccess(block: suspend (T) -> Unit): FlowCallResult<T> = apply {
        result.getOrNull()?.let { block(it) }
    }

    suspend fun onFailure(block: suspend (Throwable) -> Unit): FlowCallResult<T> = apply {
        result.exceptionOrNull()?.let { block(it) }
    }

    fun getOrNull(): T? = result.getOrNull()
    fun exceptionOrNull(): Throwable? = result.exceptionOrNull()
}

class BranchBuilder<OUT : Any> internal constructor() {
    @PublishedApi
    internal val branches = mutableListOf<suspend (OUT) -> Boolean>()
    @PublishedApi
    internal var fallback: (suspend (OUT) -> Unit)? = null

    inline fun <reified T : OUT> on(noinline handler: suspend (T) -> Unit) {
        branches += { output ->
            if (output is T) {
                handler(output)
                true
            } else {
                false
            }
        }
    }

    fun match(predicate: (OUT) -> Boolean, handler: suspend (OUT) -> Unit) {
        branches += { output ->
            if (predicate(output)) {
                handler(output)
                true
            } else {
                false
            }
        }
    }

    fun otherwise(handler: suspend (OUT) -> Unit) {
        fallback = handler
    }

    suspend fun handle(output: OUT): Boolean {
        branches.forEach { matcher ->
            if (matcher(output)) return true
        }
        fallback?.let {
            it(output)
            return true
        }
        return false
    }
}

private class FlowScriptExecutor<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    private val script: FlowScript<OUT, ENTRY>,
    private val finishFlag: FinishFlag = FinishFlag()
) {
    private var cancelInvoked = false

    suspend fun run(scriptScope: NavFlowScriptScope<OUT, ENTRY>) {
        val context = FlowStepContext(scriptScope, finishFlag)
        try {
            for (step in script.steps) {
                if (finishFlag.finished) break
                scriptScope.trace { "Step: ${step.name}" }
                step.block(context)
            }
        } catch (c: CancellationException) {
            if (!cancelInvoked) {
                cancelInvoked = true
                script.cancelHandler?.let { handler ->
                    runCatching { handler.block(context) }
                }
            }
            throw c
        }
    }
}

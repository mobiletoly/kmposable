package dev.goquick.kmposable.core.logging

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.KmposableNavState

/**
 * Optional telemetry hook invoked by [dev.goquick.kmposable.runtime.NavFlow] during lifecycle and
 * navigation events. Implement to feed logs/analytics without wiring loggers through every node.
 */
interface NavFlowLogger<OUT : Any> {
    fun onAttach(node: Node<*, *, OUT>) {}
    fun onDetach(node: Node<*, *, OUT>) {}
    fun onOutput(node: Node<*, *, OUT>, output: OUT) {}
    fun onStackChanged(state: KmposableNavState<OUT, *>) {}
}

/**
 * Optional hook for node-level exceptions surfaced via [dev.goquick.kmposable.core.StatefulNode].
 */
interface NodeErrorLogger {
    fun onNodeError(node: Any, error: Throwable) {}
}

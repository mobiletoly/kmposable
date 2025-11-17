package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackEntry

/**
 * Factory abstraction that produces fresh [NavFlow] instances for a given node container.
 * UI layers and tests can depend on the same implementation to ensure parity between headless and
 * rendered environments.
 */
fun interface NavFlowFactory<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    fun createNavFlow(): NavFlow<OUT, ENTRY>
}

/** Convenience alias for NavFlow that rely on the default stack entry implementation. */
typealias SimpleNavFlowFactory<OUT> = NavFlowFactory<OUT, DefaultStackEntry<OUT>>

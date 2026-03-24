package dev.goquick.kmposable.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.launchPushAndAwaitResult
import dev.goquick.kmposable.runtime.pushAndAwaitResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Lightweight controller for overlay-only navigation. Wraps [rememberOverlayNavFlow] and exposes
 * helpers to launch overlays that return a result.
 */
class OverlayController<OUT : Any, ENTRY : KmposableStackEntry<OUT>> internal constructor(
    val navFlow: NavFlow<OUT, ENTRY>,
    val scope: CoroutineScope
) {
    /** Pushes an overlay and awaits its first result in the current coroutine. */
    suspend fun <RESULT : Any, NODE> pushAndAwait(
        factory: () -> NODE,
        autoPop: Boolean = true
    ): KmposableResult<RESULT>
        where NODE : Node<*, *, OUT>,
              NODE : ResultNode<RESULT> = navFlow.pushAndAwaitResult(factory, autoPop)

    /** Launches an overlay in [scope], optionally auto-popping it on first result. */
    fun <RESULT : Any, NODE> launch(
        factory: () -> NODE,
        autoPop: Boolean = true,
        onResult: (KmposableResult<RESULT>) -> Unit = {}
    ): Job
        where NODE : Node<*, *, OUT>,
              NODE : ResultNode<RESULT> = scope.launchPushAndAwaitResult(navFlow, factory, autoPop, onResult)

    /** Convenience for overlays that return Unit; ignores the result payload. */
    fun <NODE> launch(
        factory: () -> NODE,
        autoPop: Boolean = true
    ): Job
        where NODE : Node<*, *, OUT>,
              NODE : ResultNode<Unit> = launch(factory, autoPop, onResult = {})
}

/**
 * Creates and remembers an [OverlayController] backed by [rememberOverlayNavFlow].
 */
@Composable
fun <OUT : Any> rememberOverlayController(
    key: Any? = Unit,
    appScope: CoroutineScope = rememberCoroutineScope()
): OverlayController<OUT, DefaultStackEntry<OUT>> {
    val navFlow = rememberOverlayNavFlow<OUT>(key = key, appScope = appScope)
    return OverlayController(navFlow = navFlow, scope = appScope)
}

/**
 * Host that renders overlay entries from an [OverlayController] using the provided [renderer].
 * This bundles [OverlayNavFlowHost] with sensible animation defaults.
 */
@Composable
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> OverlayHost(
    controller: OverlayController<OUT, ENTRY>,
    renderer: NodeRenderer<OUT>,
    enableBackHandler: Boolean = true,
    overlayScrim: (@Composable () -> Unit)? = null,
    overlayEnter: EnterTransition = fadeIn(),
    overlayExit: ExitTransition = fadeOut(),
) {
    OverlayNavFlowHost(
        navFlow = controller.navFlow,
        renderer = renderer,
        enableBackHandler = enableBackHandler,
        overlayScrim = overlayScrim,
        overlayEnter = overlayEnter,
        overlayExit = overlayExit,
    )
}

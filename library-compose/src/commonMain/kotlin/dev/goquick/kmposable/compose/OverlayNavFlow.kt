package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Remembers an overlay-only [NavFlow] without requiring callers to seed a dummy root node.
 *
 * Internally seeds a no-op root entry; overlays can then be pushed immediately.
 * Use this with [OverlayNavFlowHost] for overlay-only navigation flows.
 */
@Composable
fun <OUT : Any> rememberOverlayNavFlow(
    key: Any? = Unit,
    appScope: CoroutineScope = rememberCoroutineScope()
): NavFlow<OUT, DefaultStackEntry<OUT>> {
    val navFlow = remember(key, appScope) {
        object : NavFlow<OUT, DefaultStackEntry<OUT>>(
            appScope = appScope,
            rootNode = NoOpOverlayRoot(appScope)
        ) {
            override fun onNodeOutput(node: Node<*, *, OUT>, output: OUT) {
                // no-op: overlays should be driven externally via push/replace
            }
        }
    }
    LaunchedEffect(navFlow) { navFlow.start() }
    DisposableEffect(navFlow) {
        onDispose { navFlow.dispose() }
    }
    return navFlow
}

/**
 * Placeholder root node used to satisfy NavFlow's non-empty stack invariant.
 * Not intended to render; presentation is default (non-overlay).
 */
internal class NoOpOverlayRoot<OUT : Any>(
    parentScope: CoroutineScope
) : Node<Unit, Nothing, OUT>, PresentationAware, OverlayRootPlaceholder {
    override val presentation: Presentation = Presentation.Primary
    override val state: StateFlow<Unit> = MutableStateFlow(Unit)
    private val _outputs = MutableSharedFlow<OUT>(extraBufferCapacity = 0)
    override val outputs = _outputs.asSharedFlow()
    override fun onEvent(event: Nothing) = Unit
}

/**
 * Marker used by [OverlayNavFlowHost] to skip rendering placeholder base entries.
 */
interface OverlayRootPlaceholder

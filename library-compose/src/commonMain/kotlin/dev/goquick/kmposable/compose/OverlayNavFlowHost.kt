package dev.goquick.kmposable.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.goquick.kmposable.core.AutoCloseOverlay
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import dev.goquick.kmposable.core.nav.isOverlayPresentation
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Overlay-aware variant of [NavFlowHost] that renders a primary entry and any overlay entries
 * above it. Use when your stack contains nodes marked with `Presentation.Overlay`.
 *
 * - Finds the last non-overlay entry in the stack to treat as the base layer.
 * - Renders all entries above it that declare overlay presentation on top, in stack order.
 * - If no overlay entries exist, behaves like [NavFlowHost] (renders top only).
 */
@Composable
fun <OUT : Any> OverlayNavFlowHost(
    navFlow: NavFlow<OUT, *>,
    renderer: NodeRenderer<OUT>,
    enableBackHandler: Boolean = true,
    overlayScrim: (@Composable () -> Unit)? = null,
    overlayEnter: EnterTransition = fadeIn(),
    overlayExit: ExitTransition = fadeOut()
) {
    if (enableBackHandler) {
        KmposableBackHandler(navFlow)
    }
    val navState by navFlow.navState.collectAsState()
    val layers = navState.toOverlayLayers()
    val slots = remember { mutableStateMapOf<Any, OverlaySlot<OUT, KmposableStackEntry<OUT>>>() }
    val currentNodes = layers.overlays.map { it.node }.toSet()

    // Update slots synchronously so first composition includes overlays with animations.
    layers.overlays.forEach { entry ->
        val slot = slots.getOrPut(entry.node) {
            OverlaySlot(entry = entry, state = MutableTransitionState(false))
        }
        slot.entry = entry
        slot.state.targetState = true
    }
    // Mark vanished overlays for exit.
    slots.values
        .filter { it.entry.node !in currentNodes }
        .forEach { it.state.targetState = false }

    Box {
        if (layers.base.node !is OverlayRootPlaceholder) {
            renderer.Render(layers.base.node)
        }
        val orderedSlots = slots.values.sortedBy {
            currentNodes.indexOf(it.entry.node).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE
        }
        orderedSlots.forEach { slot ->
            val entry = slot.entry
            val enter = resolveEnter(entry, overlayEnter)
            val exit = resolveExit(entry, overlayExit)
            AnimatedVisibility(
                visibleState = slot.state,
                enter = enter,
                exit = exit,
                label = "overlay-enter"
            ) {
                if (slot.state.currentState || slot.state.targetState) {
                    overlayScrim?.invoke()
                }
                renderer.Render(entry.node)
                AutoCloseEffect(entry.node, navFlow)
                LaunchedEffect(slot.state.isIdle, slot.state.targetState) {
                    if (!slot.state.targetState && slot.state.isIdle) {
                        slots.remove(entry.node)
                    }
                }
            }
        }
    }
}

@Composable
private fun <OUT : Any> AutoCloseEffect(node: Node<*, *, OUT>, navFlow: NavFlow<OUT, *>) {
    val autoClose = node as? AutoCloseOverlay<*> ?: return
    val flow by rememberUpdatedState(navFlow)
    LaunchedEffect(autoClose, flow) {
        runAutoCloseOverlay(flow, autoClose)
    }
}

internal suspend fun <OUT : Any> runAutoCloseOverlay(
    navFlow: NavFlow<OUT, *>,
    node: AutoCloseOverlay<*>
): Boolean {
    @Suppress("UNCHECKED_CAST")
    val typed = node as AutoCloseOverlay<Any>
    val result = typed.result.firstOrNull() ?: return false
    if (!typed.shouldAutoClose(result)) return false
    if (!navFlow.isStarted()) return false
    val isStillTop = runCatching { navFlow.currentTopNode() === node }.getOrDefault(false)
    if (!isStillTop) return false
    return navFlow.canPop() && navFlow.pop()
}

internal data class OverlayLayers<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    val base: ENTRY,
    val overlays: List<ENTRY>
)

internal fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> KmposableNavState<OUT, ENTRY>.toOverlayLayers(): OverlayLayers<OUT, ENTRY> {
    val idxBase = stack.indexOfLast { !it.node.isOverlayPresentation() }.takeIf { it >= 0 } ?: stack.lastIndex
    val base = stack[idxBase]
    val overlays = if (idxBase < stack.lastIndex) stack.subList(idxBase + 1, stack.size) else emptyList()
    return OverlayLayers(base, overlays)
}

private data class OverlaySlot<OUT : Any, ENTRY : KmposableStackEntry<OUT>>(
    var entry: ENTRY,
    val state: MutableTransitionState<Boolean>
)

private fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> resolveEnter(
    entry: ENTRY,
    default: EnterTransition
): EnterTransition {
    val aware = entry.node as? OverlayAnimationAware ?: return default
    return aware.overlayEnter ?: default
}

private fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> resolveExit(
    entry: ENTRY,
    default: ExitTransition
): ExitTransition {
    val aware = entry.node as? OverlayAnimationAware ?: return default
    return aware.overlayExit ?: default
}

/**
 * Optional animation hints for overlay nodes. Hosts can consult these to use per-node enter/exit
 * transitions instead of the global defaults.
 */
interface OverlayAnimationAware : PresentationAware {
    override val presentation: Presentation get() = Presentation.Overlay
    val overlayEnter: EnterTransition? get() = null
    val overlayExit: ExitTransition? get() = null
}

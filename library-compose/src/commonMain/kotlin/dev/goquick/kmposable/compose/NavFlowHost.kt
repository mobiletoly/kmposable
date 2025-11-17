package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goquick.kmposable.runtime.NavFlow

/** Renders the top kmposable node and optionally wires the shared back handler. */
@Composable
fun <OUT : Any> NavFlowHost(
    navFlow: NavFlow<OUT, *>,
    renderer: NodeRenderer<OUT>,
    enableBackHandler: Boolean = true,
) {
    if (enableBackHandler) {
        KmposableBackHandler(navFlow)
    }
    val navState by navFlow.navState.collectAsState()
    renderer.Render(navState.top)
}

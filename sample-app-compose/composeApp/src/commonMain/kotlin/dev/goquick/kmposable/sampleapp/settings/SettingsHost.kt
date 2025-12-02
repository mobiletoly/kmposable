package dev.goquick.kmposable.sampleapp.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.goquick.kmposable.compose.NodeHost
import dev.goquick.kmposable.compose.OverlayHost
import dev.goquick.kmposable.compose.nodeRenderer
import dev.goquick.kmposable.compose.rememberOverlayController
import dev.goquick.kmposable.sampleapp.contacts.ui.SettingsScreen
import dev.goquick.kmposable.sampleapp.settings.overlay.SettingsOverlayHost
import dev.goquick.kmposable.sampleapp.settings.overlay.SettingsOverlayNode

/**
 * Settings host keeps UI pure and wires the node's state/events to the screen.
 * Even for small nodes, keeping the Host layer consistent makes NavFlow renderer wiring predictable.
 */
@Composable
fun SettingsHost(node: SettingsNode) {
    val overlayController = rememberOverlayController<Unit>()
    val overlayRenderer = remember {
        nodeRenderer<Unit> {
            registerResultOnly<SettingsOverlayNode> { SettingsOverlayHost(it) }
        }
    }

    NodeHost(node = node) { state, onEvent, _ ->
        SettingsScreen(
            state = state,
            onEvent = onEvent,
            onShowOverlay = {
                overlayController.launch(
                    factory = { SettingsOverlayNode(parentScope = overlayController.scope) },
                    autoPop = false
                )
            }
        )
    }
    OverlayHost(controller = overlayController, renderer = overlayRenderer)
}

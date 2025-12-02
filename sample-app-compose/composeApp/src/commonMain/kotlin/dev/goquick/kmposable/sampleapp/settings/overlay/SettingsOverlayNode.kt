package dev.goquick.kmposable.sampleapp.settings.overlay

import dev.goquick.kmposable.core.AutoCloseOverlay
import dev.goquick.kmposable.core.ResultfulStatefulNode
import dev.goquick.kmposable.core.nav.Presentation
import dev.goquick.kmposable.core.nav.PresentationAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsOverlayNode(parentScope: CoroutineScope) :
    ResultfulStatefulNode<SettingsOverlayState, SettingsOverlayEvent, Nothing, Unit>(
        parentScope = parentScope,
        initialState = SettingsOverlayState()
    ),
    AutoCloseOverlay<Unit>,
    PresentationAware {

    override val presentation: Presentation = Presentation.Overlay

    override fun onEvent(event: SettingsOverlayEvent) {
        when (event) {
            SettingsOverlayEvent.Confirm -> scope.launch { emitOk(Unit) }
            SettingsOverlayEvent.Dismiss -> scope.launch { emitCanceled() }
        }
    }
}

data class SettingsOverlayState(
    val title: String = "Overlay auto-close",
    val message: String = "This overlay pops itself after emitting a result."
)

sealed interface SettingsOverlayEvent {
    data object Confirm : SettingsOverlayEvent
    data object Dismiss : SettingsOverlayEvent
}

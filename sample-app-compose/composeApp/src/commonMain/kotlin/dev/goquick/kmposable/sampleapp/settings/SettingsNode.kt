package dev.goquick.kmposable.sampleapp.settings

import dev.goquick.kmposable.core.StatefulNode
import kotlinx.coroutines.CoroutineScope

class SettingsNode(parentScope: CoroutineScope) :
    StatefulNode<SettingsState, SettingsEvent, Nothing>(
        parentScope = parentScope,
        initialState = SettingsState()
    ) {

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.AllowSyncChanged -> setState(state.value.copy(allowSync = event.enabled))
        }
    }
}

data class SettingsState(val allowSync: Boolean = true)

sealed interface SettingsEvent {
    data class AllowSyncChanged(val enabled: Boolean) : SettingsEvent
}

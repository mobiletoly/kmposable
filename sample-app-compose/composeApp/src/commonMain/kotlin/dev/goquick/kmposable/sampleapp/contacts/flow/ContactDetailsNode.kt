package dev.goquick.kmposable.sampleapp.contacts.flow

import dev.goquick.kmposable.core.EffectfulStatefulNode
import dev.goquick.kmposable.sampleapp.contacts.Contact
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.ContactsRepository
import kotlinx.coroutines.launch

sealed interface ContactDetailsEffect {
    data class ShowMessage(val text: String) : ContactDetailsEffect
}

/**
 * Effectful node for a single contact: keeps data loading logic headless and emits outputs
 * for edit/delete/back while effects carry transient UI signals (snackbars).
 */
class ContactDetailsNode(
    private val contactId: ContactId,
    private val repository: ContactsRepository,
    parentScope: kotlinx.coroutines.CoroutineScope
) : EffectfulStatefulNode<ContactDetailsState, ContactDetailsEvent, ContactsFlowEvent, ContactDetailsEffect>(
    parentScope = parentScope,
    initialState = ContactDetailsState(isLoading = true)
) {

    override fun onAttach() {
        super.onAttach()
        scope.launch { loadContact() }
    }

    override fun onEvent(event: ContactDetailsEvent) {
        when (event) {
            ContactDetailsEvent.Refresh -> scope.launch { loadContact() }
            ContactDetailsEvent.EditClicked -> scope.launch {
                val contact = state.value.contact ?: return@launch
                emitOutput(ContactsFlowEvent.OpenEditor(contact))
            }
            ContactDetailsEvent.DeleteClicked -> scope.launch {
                emitEffect(ContactDetailsEffect.ShowMessage("Deleted"))
                emitOutput(ContactsFlowEvent.DeleteContact(contactId))
            }
            ContactDetailsEvent.BackClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.NavigateBack)
            }
        }
    }

    private suspend fun loadContact() {
        runCatchingState(
            onStart = { it.copy(isLoading = true, error = null) },
            onSuccess = { state, contact ->
                state.copy(
                    isLoading = false,
                    contact = contact,
                    error = if (contact == null) "Contact not found" else null
                )
            },
            onError = { state, error ->
                state.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load contact"
                )
            }
        ) { repository.getById(contactId) }
            .onFailure { emitEffect(ContactDetailsEffect.ShowMessage("Unable to load contact")) }
    }
}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val error: String? = null
)

sealed interface ContactDetailsEvent {
    data object Refresh : ContactDetailsEvent
    data object EditClicked : ContactDetailsEvent
    data object DeleteClicked : ContactDetailsEvent
    data object BackClicked : ContactDetailsEvent
}

package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.StatefulNode
import kotlinx.coroutines.launch

class ContactDetailsNode(
    val contactId: ContactId,
    parentScope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<ContactDetailsState, ContactDetailsEvent, ContactsFlowEvent>(
    parentScope = parentScope,
    initialState = ContactDetailsState(isLoading = true)
) {

    override fun onEvent(event: ContactDetailsEvent) {
        when (event) {
            ContactDetailsEvent.EditClicked -> scope.launch {
                val contact = state.value.contact ?: return@launch
                emitOutput(ContactsFlowEvent.OpenEditor(contact))
            }
            ContactDetailsEvent.DeleteClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.DeleteContact(contactId))
            }
            ContactDetailsEvent.BackClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.NavigateBack)
            }
        }
    }

    fun showLoading() {
        updateState { it.copy(isLoading = true, error = null) }
    }

    fun showContact(contact: Contact) {
        updateState { it.copy(isLoading = false, contact = contact, error = null) }
    }

    fun showError(message: String) {
        updateState { it.copy(isLoading = false, error = message) }
    }
}

data class ContactDetailsState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val error: String? = null
)

sealed interface ContactDetailsEvent {
    data object EditClicked : ContactDetailsEvent
    data object DeleteClicked : ContactDetailsEvent
    data object BackClicked : ContactDetailsEvent
}

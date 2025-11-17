package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.StatefulNode
import kotlinx.coroutines.launch

class ContactDetailsNode(
    private val contactId: ContactId,
    private val repository: ContactsRepository,
    parentScope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<ContactDetailsState, ContactDetailsEvent, ContactsFlowEvent>(
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
                emitOutput(ContactsFlowEvent.DeleteContact(contactId))
            }
            ContactDetailsEvent.BackClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.NavigateBack)
            }
        }
    }

    private suspend fun loadContact() {
        updateState { it.copy(isLoading = true, error = null) }
        try {
            val contact = repository.getById(contactId)
            updateState {
                it.copy(
                    isLoading = false,
                    contact = contact,
                    error = if (contact == null) "Contact not found" else null
                )
            }
        } catch (t: Throwable) {
            updateState {
                it.copy(isLoading = false, error = t.message ?: "Unable to load contact")
            }
        }
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

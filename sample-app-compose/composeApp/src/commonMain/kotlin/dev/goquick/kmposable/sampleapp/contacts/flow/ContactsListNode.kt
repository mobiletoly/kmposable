package dev.goquick.kmposable.sampleapp.contacts.flow

import dev.goquick.kmposable.core.StatefulNode
import dev.goquick.kmposable.sampleapp.contacts.Contact
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.ContactsRepository
import kotlinx.coroutines.launch

/**
 * Headless list node: exposes list/search state and emits navigation outputs.
 * UI lives elsewhere; this stays pure Kotlin for reuse/testing.
 */
class ContactsListNode(
    private val repository: ContactsRepository,
    parentScope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<ContactsListState, ContactsListEvent, ContactsFlowEvent>(
    parentScope = parentScope,
    initialState = ContactsListState()
) {

    override fun onAttach() {
        super.onAttach()
        scope.launch { refreshContacts() }
    }

    override fun onEvent(event: ContactsListEvent) {
        when (event) {
            ContactsListEvent.ScreenStarted -> scope.launch { refreshContacts() }
            is ContactsListEvent.SearchChanged -> updateState { it.copy(query = event.query) }
            is ContactsListEvent.ContactClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.OpenContact(event.id))
            }
            ContactsListEvent.AddNewClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.CreateContact)
            }
        }
    }

    private suspend fun refreshContacts() {
        runCatchingState(
            onStart = { it.copy(isLoading = true, error = null) },
            onEach = { state, contacts -> state.copy(isLoading = false, contacts = contacts) },
            onError = { state, error ->
                state.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load contacts"
                )
            }
        ) { repository.getAll() }
    }
}

data class ContactsListState(
    val isLoading: Boolean = true,
    val contacts: List<Contact> = emptyList(),
    val query: String = "",
    val error: String? = null
)

sealed interface ContactsListEvent {
    data object ScreenStarted : ContactsListEvent
    data class SearchChanged(val query: String) : ContactsListEvent
    data class ContactClicked(val id: ContactId) : ContactsListEvent
    data object AddNewClicked : ContactsListEvent
}

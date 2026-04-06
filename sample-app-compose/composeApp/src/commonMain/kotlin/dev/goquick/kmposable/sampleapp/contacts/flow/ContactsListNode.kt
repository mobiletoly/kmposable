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
    private var allContacts: List<Contact> = emptyList()

    override fun onAttach() {
        super.onAttach()
        scope.launch { refreshContacts() }
    }

    override fun onEvent(event: ContactsListEvent) {
        when (event) {
            ContactsListEvent.ScreenStarted -> scope.launch { refreshContacts() }
            is ContactsListEvent.SearchChanged -> {
                updateState { state ->
                    state.copy(
                        query = event.query,
                        contacts = allContacts.filteredBy(event.query)
                    )
                }
            }
            is ContactsListEvent.ContactClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.OpenContact(event.id))
            }
            ContactsListEvent.AddNewClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.CreateContact)
            }
            ContactsListEvent.SettingsClicked -> scope.launch {
                emitOutput(ContactsFlowEvent.OpenSettings)
            }
        }
    }

    private suspend fun refreshContacts() {
        runCatchingState(
            onStart = { it.copy(isLoading = true, error = null) },
            onSuccess = { state, contacts ->
                allContacts = contacts
                state.copy(
                    isLoading = false,
                    contacts = contacts.filteredBy(state.query)
                )
            },
            onError = { state, error ->
                state.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load contacts"
                )
            }
        ) { repository.getAll() }
    }
}

private fun List<Contact>.filteredBy(query: String): List<Contact> {
    if (query.isBlank()) return this
    return filter { contact -> contact.name.contains(query, ignoreCase = true) }
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
    data object SettingsClicked : ContactsListEvent
}

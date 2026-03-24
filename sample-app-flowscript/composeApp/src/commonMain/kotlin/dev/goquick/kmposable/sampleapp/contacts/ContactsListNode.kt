package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.StatefulNode
import kotlinx.coroutines.launch

class ContactsListNode(
    parentScope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<ContactsListState, ContactsListEvent, ContactsFlowEvent>(
    parentScope = parentScope,
    initialState = ContactsListState()
) {
    private var allContacts: List<Contact> = emptyList()

    override fun onEvent(event: ContactsListEvent) {
        when (event) {
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
        }
    }

    fun showLoading() {
        updateState { it.copy(isLoading = true, error = null) }
    }

    fun showContacts(contacts: List<Contact>) {
        allContacts = contacts
        updateState { state ->
            state.copy(
                isLoading = false,
                contacts = contacts.filteredBy(state.query),
                error = null
            )
        }
    }

    fun showError(message: String) {
        updateState { it.copy(isLoading = false, error = message) }
    }
}

data class ContactsListState(
    val isLoading: Boolean = true,
    val contacts: List<Contact> = emptyList(),
    val query: String = "",
    val error: String? = null
)

sealed interface ContactsListEvent {
    data class SearchChanged(val query: String) : ContactsListEvent
    data class ContactClicked(val id: ContactId) : ContactsListEvent
    data object AddNewClicked : ContactsListEvent
}

private fun List<Contact>.filteredBy(query: String): List<Contact> {
    if (query.isBlank()) return this
    return filter { contact -> contact.name.contains(query, ignoreCase = true) }
}

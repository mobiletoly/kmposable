package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single-flow runtime that owns list, details, and edit nodes for the Contacts feature.
 */
class ContactsNavFlow(
    private val repository: ContactsRepository,
    private val appScope: CoroutineScope
) : NavFlow<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>(
    appScope = appScope,
    rootNode = ContactsListNode(repository, appScope)
) {

    override fun onNodeOutput(node: Node<*, *, ContactsFlowEvent>, output: ContactsFlowEvent) {
        when (output) {
            is ContactsFlowEvent.OpenContact -> push(ContactDetailsNode(output.id, repository, appScope))
            ContactsFlowEvent.CreateContact -> push(EditContactNode(existingContact = null, repository = repository, parentScope = appScope))
            ContactsFlowEvent.NavigateBack -> pop()
            is ContactsFlowEvent.OpenEditor -> push(EditContactNode(existingContact = output.contact, repository = repository, parentScope = appScope))
            is ContactsFlowEvent.DeleteContact -> appScope.launch {
                repository.delete(output.id)
                pop()
                refreshList()
            }
            is ContactsFlowEvent.ContactSaved -> {
                pop()
                refreshList()
                refreshDetailsIfVisible()
            }
            ContactsFlowEvent.EditorCancelled -> pop()
        }
    }

    private fun refreshList() {
        (navState.value.root as? ContactsListNode)?.onEvent(ContactsListEvent.ScreenStarted)
    }

    private fun refreshDetailsIfVisible() {
        val top = currentTopNode() as? ContactDetailsNode ?: return
        top.onEvent(ContactDetailsEvent.Refresh)
    }
}

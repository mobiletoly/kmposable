package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackNavigator
import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.flow.first
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
    rootNode = ContactsListNode(repository, appScope),
    navigatorFactory = { entry -> KmposableStackNavigator(entry) }
) {

    override fun onNodeOutput(node: Node<*, *, ContactsFlowEvent>, output: ContactsFlowEvent) {
        when (output) {
            is ContactsFlowEvent.OpenContact -> push(ContactDetailsNode(output.id, repository, appScope))
            ContactsFlowEvent.CreateContact -> launchEditor(existingContact = null)
            ContactsFlowEvent.NavigateBack -> pop()
            is ContactsFlowEvent.OpenEditor -> launchEditor(existingContact = output.contact)
            is ContactsFlowEvent.DeleteContact -> appScope.launch {
                repository.delete(output.id)
                pop()
                refreshList()
            }
            is ContactsFlowEvent.ContactSaved -> Unit // handled via pushForResult
            ContactsFlowEvent.EditorCancelled -> pop()
        }
    }

    private fun launchEditor(existingContact: Contact?) {
        appScope.launch {
            val node = EditContactNode(existingContact = existingContact, repository = repository, parentScope = appScope)
            push(node)
            val result = node.result.first()
            pop()
            when (result) {
                is KmposableResult.Ok<*> -> {
                    refreshList()
                    refreshDetailsIfVisible()
                }
                KmposableResult.Canceled -> {
                    // already popped; nothing else
                }
            }
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

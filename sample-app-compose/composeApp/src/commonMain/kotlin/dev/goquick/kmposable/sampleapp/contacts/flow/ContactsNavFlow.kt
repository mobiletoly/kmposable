package dev.goquick.kmposable.sampleapp.contacts.flow

import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackNavigator
import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.ContactsRepository
import dev.goquick.kmposable.runtime.pushAndAwaitResultOnly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single-flow runtime that owns list, details, and edit nodes for the Contacts feature.
 * Lives in the `contacts.flow` package to keep headless logic separate from UI/hosts.
 */
class ContactsNavFlow(
    private val repository: ContactsRepository,
    private val appScope: CoroutineScope
) : NavFlow<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>(
    appScope = appScope,
    rootNode = ContactsListNode(repository, appScope),
    navigatorFactory = { entry -> KmposableStackNavigator(entry) }
) {

    override fun onNodeOutput(node: dev.goquick.kmposable.core.Node<*, *, ContactsFlowEvent>, output: ContactsFlowEvent) {
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
            ContactsFlowEvent.OpenSettings -> Unit
        }
    }

    private fun launchEditor(existingContact: dev.goquick.kmposable.sampleapp.contacts.Contact?) {
        appScope.launch {
            val result = pushAndAwaitResultOnly(
                factory = {
                    EditContactNode(
                        existingContact = existingContact,
                        repository = repository,
                        parentScope = appScope
                    )
                }
            )
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

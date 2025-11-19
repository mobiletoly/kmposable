package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.runtime.NavFlowScriptScope
import dev.goquick.kmposable.runtime.awaitOutputCase
import dev.goquick.kmposable.runtime.launchNavFlowScript
import dev.goquick.kmposable.runtime.pushForResult
import dev.goquick.kmposable.runtime.runCatchingNodeCall
import dev.goquick.kmposable.runtime.withNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

fun createContactsNavFlow(
    repository: ContactsRepository,
    appScope: CoroutineScope
): NavFlow<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>> =
    NavFlow(
        appScope = appScope,
        rootNode = ContactsListNode(appScope)
    )

fun NavFlow<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.launchContactsScript(
    scriptScope: CoroutineScope,
    nodeScope: CoroutineScope,
    repository: ContactsRepository,
    onTrace: ((String) -> Unit)? = null
): Job = launchNavFlowScript(scriptScope, onTrace) {
    runContactsFlowScript(repository, nodeScope)
}

suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.runContactsFlowScript(
    repository: ContactsRepository,
    nodeScope: CoroutineScope
) {
    while (true) {
        showContactsList(repository)
        when (val action = awaitListAction()) {
            is ListAction.Open -> runDetailsFlow(action.id, repository, nodeScope)
            ListAction.Create -> runEditorFlow(existing = null, repository = repository, nodeScope = nodeScope)
        }
    }
}

private suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.showContactsList(
    repository: ContactsRepository
) {
    val listNode = navFlow.navState.value.root as ContactsListNode
    runCatchingNodeCall(
        onLoading = listNode::showLoading,
        onSuccess = listNode::showContacts,
        onError = { listNode.showError(it.message ?: "Unable to load contacts") }
    ) {
        repository.getAll()
    }
}

private suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.runDetailsFlow(
    id: ContactId,
    repository: ContactsRepository,
    nodeScope: CoroutineScope
) {
    withNode(factory = { ContactDetailsNode(id, nodeScope) }) {
        showLoading()
        var currentContact = loadContact(repository)
        if (currentContact == null) return@withNode
        showContact(currentContact)

        while (true) {
            when (val action = awaitDetailsAction()) {
                is DetailsAction.Edit -> {
                    val updated = runEditorFlow(action.contact, repository, nodeScope)
                    if (updated != null) {
                        currentContact = updated
                        showContact(currentContact)
                    }
                }
                is DetailsAction.Delete -> {
                    repository.delete(action.id)
                    return@withNode
                }
                DetailsAction.Back -> {
                    return@withNode
                }
            }
        }
    }
}

private suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.runEditorFlow(
    existing: Contact?,
    repository: ContactsRepository,
    nodeScope: CoroutineScope
): Contact? {
    return when (val result = pushForResult(
        factory = { EditContactNode(existingContact = existing, parentScope = nodeScope) },
        mapper = { event ->
            when (event) {
                is ContactsFlowEvent.ContactSaved -> EditorResult.Saved(event.contact)
                ContactsFlowEvent.EditorCancelled -> EditorResult.Cancelled
                else -> null
            }
        }
    )) {
        is EditorResult.Saved -> {
            repository.upsert(result.contact)
            result.contact
        }
        EditorResult.Cancelled -> null
    }
}

private sealed interface ListAction {
    data class Open(val id: ContactId) : ListAction
    data object Create : ListAction
}

private suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.awaitListAction(): ListAction =
    awaitOutputCase {
        on<ContactsFlowEvent.OpenContact> { ListAction.Open(it.id) }
        on<ContactsFlowEvent.CreateContact> { ListAction.Create }
    }

private sealed interface DetailsAction {
    data class Edit(val contact: Contact?) : DetailsAction
    data class Delete(val id: ContactId) : DetailsAction
    data object Back : DetailsAction
}

private suspend fun NavFlowScriptScope<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>.awaitDetailsAction(): DetailsAction =
    awaitOutputCase {
        on<ContactsFlowEvent.OpenEditor> { DetailsAction.Edit(it.contact) }
        on<ContactsFlowEvent.DeleteContact> { DetailsAction.Delete(it.id) }
        on<ContactsFlowEvent.NavigateBack> { DetailsAction.Back }
    }

private sealed interface EditorResult {
    data class Saved(val contact: Contact) : EditorResult
    data object Cancelled : EditorResult
}

private suspend fun ContactDetailsNode.loadContact(repository: ContactsRepository): Contact? {
    return runCatching { repository.getById(contactId) }.getOrElse {
        showError(it.message ?: "Unable to load contact")
        null
    } ?: run {
        showError("Contact not found")
        null
    }
}

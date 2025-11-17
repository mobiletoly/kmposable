package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.core.StatefulNode
import kotlinx.coroutines.launch

class EditContactNode(
    private val existingContact: Contact?,
    private val repository: ContactsRepository,
    parentScope: kotlinx.coroutines.CoroutineScope
) : StatefulNode<EditContactState, EditContactEvent, ContactsFlowEvent>(
    parentScope = parentScope,
    initialState = EditContactState.fromContact(existingContact)
) {

    override fun onEvent(event: EditContactEvent) {
        when (event) {
            is EditContactEvent.NameChanged -> updateState { it.copy(name = event.value, error = null) }
            is EditContactEvent.PhoneChanged -> updateState { it.copy(phone = event.value, error = null) }
            is EditContactEvent.EmailChanged -> updateState { it.copy(email = event.value, error = null) }
            EditContactEvent.CancelClicked -> scope.launch { emitOutput(ContactsFlowEvent.EditorCancelled) }
            EditContactEvent.SaveClicked -> scope.launch { saveContact() }
        }
    }

    private suspend fun saveContact() {
        val current = state.value
        val validationError = validate(current)
        if (validationError != null) {
            updateState { it.copy(error = validationError) }
            return
        }
        updateState { it.copy(isSaving = true, error = null) }
        try {
            val contact = Contact(
                id = existingContact?.id ?: ContactId(current.generatedId ?: randomId()),
                name = current.name.trim(),
                phone = current.phone.trim(),
                email = current.email?.trim().takeUnless { it.isNullOrBlank() }
            )
            repository.upsert(contact)
            emitOutput(ContactsFlowEvent.ContactSaved(contact))
        } catch (t: Throwable) {
            updateState { it.copy(isSaving = false, error = t.message ?: "Failed to save contact") }
        }
    }

    private fun validate(state: EditContactState): String? {
        if (state.name.isBlank()) return "Name is required"
        if (state.phone.isBlank()) return "Phone is required"
        return null
    }

    private fun randomId(): String = kotlin.random.Random.nextInt(1_000_000).toString()
}

data class EditContactState(
    val name: String,
    val phone: String,
    val email: String?,
    val isSaving: Boolean = false,
    val error: String? = null,
    val generatedId: String? = null
) {
    companion object {
        fun fromContact(contact: Contact?): EditContactState = if (contact == null) {
            EditContactState(name = "", phone = "", email = null)
        } else {
            EditContactState(
                name = contact.name,
                phone = contact.phone,
                email = contact.email,
                generatedId = contact.id.raw
            )
        }
    }
}

sealed interface EditContactEvent {
    data object SaveClicked : EditContactEvent
    data object CancelClicked : EditContactEvent
    data class NameChanged(val value: String) : EditContactEvent
    data class PhoneChanged(val value: String) : EditContactEvent
    data class EmailChanged(val value: String?) : EditContactEvent
}

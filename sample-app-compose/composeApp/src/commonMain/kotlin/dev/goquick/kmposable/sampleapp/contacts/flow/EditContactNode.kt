package dev.goquick.kmposable.sampleapp.contacts.flow

import dev.goquick.kmposable.core.EffectfulStatefulNode
import dev.goquick.kmposable.core.KmposableResult
import dev.goquick.kmposable.core.ResultNode
import dev.goquick.kmposable.sampleapp.contacts.Contact
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.ContactsRepository
import kotlinx.coroutines.launch

sealed interface EditContactEffect {
    data class ShowMessage(val text: String) : EditContactEffect
}

/**
 * Result-only node for editing/creating a contact. Emits transient effects for UX messaging
 * and a KmposableResult for the parent NavFlow to react to. Kept headless for reuse in tests.
 */
class EditContactNode(
    private val existingContact: Contact?,
    private val repository: ContactsRepository,
    parentScope: kotlinx.coroutines.CoroutineScope
) : EffectfulStatefulNode<EditContactState, EditContactEvent, ContactsFlowEvent, EditContactEffect>(
    parentScope = parentScope,
    initialState = EditContactState.fromContact(existingContact)
), ResultNode<Contact> {

    private val _result = kotlinx.coroutines.flow.MutableSharedFlow<KmposableResult<Contact>>(replay = 1)
    override val result = _result
    private var resultEmitted = false

    override fun onEvent(event: EditContactEvent) {
        when (event) {
            is EditContactEvent.NameChanged -> updateState { it.copy(name = event.value, error = null) }
            is EditContactEvent.PhoneChanged -> updateState { it.copy(phone = event.value, error = null) }
            is EditContactEvent.EmailChanged -> updateState { it.copy(email = event.value, error = null) }
            EditContactEvent.CancelClicked -> scope.launch { emitResult(KmposableResult.Canceled) }
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
            emitEffect(EditContactEffect.ShowMessage("Saved"))
            emitResult(KmposableResult.Ok(contact))
        } catch (t: Throwable) {
            updateState { it.copy(isSaving = false, error = t.message ?: "Failed to save contact") }
            emitEffect(EditContactEffect.ShowMessage("Failed to save contact"))
        }
    }

    private suspend fun emitResult(value: KmposableResult<Contact>) {
        if (resultEmitted) return
        resultEmitted = true
        _result.emit(value)
    }

    private fun tryEmitResult(value: KmposableResult<Contact>) {
        if (resultEmitted) return
        resultEmitted = _result.tryEmit(value) || resultEmitted
    }

    override fun onDetach() {
        tryEmitResult(KmposableResult.Canceled)
        super.onDetach()
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

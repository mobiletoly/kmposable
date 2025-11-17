package dev.goquick.kmposable.sampleapp.contacts

/** Outputs raised by any node inside the Contacts flow. */
sealed interface ContactsFlowEvent {
    data class OpenContact(val id: ContactId) : ContactsFlowEvent
    data object CreateContact : ContactsFlowEvent
    data object NavigateBack : ContactsFlowEvent
    data class OpenEditor(val contact: Contact?) : ContactsFlowEvent
    data class DeleteContact(val id: ContactId) : ContactsFlowEvent
    data class ContactSaved(val contact: Contact) : ContactsFlowEvent
    data object EditorCancelled : ContactsFlowEvent
}

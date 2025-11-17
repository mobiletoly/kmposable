package dev.goquick.kmposable.sampleapp.contacts

data class ContactId(val raw: String)

data class Contact(
    val id: ContactId,
    val name: String,
    val phone: String,
    val email: String?
)

interface ContactsRepository {
    suspend fun getAll(): List<Contact>
    suspend fun getById(id: ContactId): Contact?
    suspend fun upsert(contact: Contact)
    suspend fun delete(id: ContactId)
}

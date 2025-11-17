package dev.goquick.kmposable.sampleapp.contacts

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryContactsRepository(contacts: List<Contact> = emptyList()) : ContactsRepository {
    private val mutex = Mutex()
    private val items = contacts.associateBy { it.id.raw }.toMutableMap()

    override suspend fun getAll(): List<Contact> = mutex.withLock {
        items.values.sortedBy { it.name }
    }

    override suspend fun getById(id: ContactId): Contact? = mutex.withLock {
        items[id.raw]
    }

    override suspend fun upsert(contact: Contact) {
        mutex.withLock {
            items[contact.id.raw] = contact
        }
    }

    override suspend fun delete(id: ContactId) {
        mutex.withLock {
            items.remove(id.raw)
        }
    }
}

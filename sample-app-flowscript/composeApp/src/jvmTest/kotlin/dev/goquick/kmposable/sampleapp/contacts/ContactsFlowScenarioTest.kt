package dev.goquick.kmposable.sampleapp.contacts

import dev.goquick.kmposable.runtime.SimpleNavFlowFactory
import dev.goquick.kmposable.test.createTestScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsFlowScenarioTest {

    @Test
    fun openingContactPushesDetailsNode() = runTest {
        val contact = Contact(ContactId("1"), "Alice", "123", null)
        val repository = InMemoryContactsRepository(listOf(contact))
        val factory = SimpleNavFlowFactory<ContactsFlowEvent> {
            createContactsNavFlow(repository, this)
        }

        val scenario = factory.createTestScenario(this).start()
        val scriptJob = scenario.navFlow.launchContactsScript(
            scriptScope = this,
            nodeScope = this,
            repository = repository
        )
        advanceUntilIdle()

        scenario.send(ContactsListEvent.ContactClicked(contact.id))
        advanceUntilIdle()

        scenario.assertTopNodeIs<ContactDetailsNode>()
        scriptJob.cancel()
        scenario.finish()
    }

    @Test
    fun editingContactReturnsToDetails() = runTest {
        val contact = Contact(ContactId("10"), "Bob", "555", null)
        val repository = InMemoryContactsRepository(listOf(contact))
        val factory = SimpleNavFlowFactory<ContactsFlowEvent> {
            createContactsNavFlow(repository, this)
        }

        val scenario = factory.createTestScenario(this).start()
        val scriptJob = scenario.navFlow.launchContactsScript(
            scriptScope = this,
            nodeScope = this,
            repository = repository
        )
        advanceUntilIdle()

        scenario.send(ContactsListEvent.ContactClicked(contact.id))
        advanceUntilIdle()
        scenario.assertTopNodeIs<ContactDetailsNode>()

        scenario.send(ContactDetailsEvent.EditClicked)
        advanceUntilIdle()
        scenario.assertTopNodeIs<EditContactNode>()

        scenario.send(EditContactEvent.NameChanged("Bob Updated"))
        scenario.send(EditContactEvent.PhoneChanged("777-000"))
        advanceUntilIdle()
        scenario.send(EditContactEvent.SaveClicked)
        advanceUntilIdle()

        scenario.assertTopNodeIs<ContactDetailsNode>()
        scriptJob.cancel()
        scenario.finish()
    }
}

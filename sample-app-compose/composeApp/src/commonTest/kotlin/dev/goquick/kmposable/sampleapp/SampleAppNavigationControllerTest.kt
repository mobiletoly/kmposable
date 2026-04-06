package dev.goquick.kmposable.sampleapp

import androidx.navigation3.runtime.NavKey
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsFlowEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleAppNavigationControllerTest {

    @Test
    fun openSettingsOutputPushesSettingsRouteSingleTop() {
        val backStack: MutableList<NavKey> = mutableListOf(ContactsRoute)
        val controller = SampleAppNavigationController(backStack)

        controller.onContactsOutput(ContactsFlowEvent.OpenSettings)
        controller.onContactsOutput(ContactsFlowEvent.OpenSettings)

        assertEquals(
            listOf<NavKey>(ContactsRoute, SettingsRoute),
            backStack,
        )
    }

    @Test
    fun navigateBackPopsWhenSecondaryDestinationIsVisible() {
        val backStack: MutableList<NavKey> = mutableListOf(ContactsRoute, SettingsRoute)
        val controller = SampleAppNavigationController(backStack)

        val changed = controller.navigateBack()

        assertTrue(changed)
        assertEquals(listOf<NavKey>(ContactsRoute), backStack)
    }

    @Test
    fun navigateBackDoesNothingAtRoot() {
        val backStack: MutableList<NavKey> = mutableListOf(ContactsRoute)
        val controller = SampleAppNavigationController(backStack)

        val changed = controller.navigateBack()

        assertFalse(changed)
        assertEquals(listOf<NavKey>(ContactsRoute), backStack)
    }
}

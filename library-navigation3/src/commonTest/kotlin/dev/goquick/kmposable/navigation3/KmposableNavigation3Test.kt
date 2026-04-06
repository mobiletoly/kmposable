package dev.goquick.kmposable.navigation3

import androidx.savedstate.serialization.SavedStateConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.modules.EmptySerializersModule

class KmposableNavigation3Test {

    @Test
    fun pushSingleTopPushesWhenDestinationDiffers() {
        val backStack = mutableListOf("contacts")

        val changed = backStack.pushSingleTop("settings")

        assertTrue(changed)
        assertEquals(listOf("contacts", "settings"), backStack)
    }

    @Test
    fun pushSingleTopSkipsDuplicateTopDestination() {
        val backStack = mutableListOf("contacts", "settings")

        val changed = backStack.pushSingleTop("settings")

        assertFalse(changed)
        assertEquals(listOf("contacts", "settings"), backStack)
    }

    @Test
    fun navKeySavedStateConfigurationUsesProvidedModule() {
        val configuration = navKeySavedStateConfiguration(EmptySerializersModule())

        assertNotEquals(
            SavedStateConfiguration.DEFAULT.serializersModule,
            configuration.serializersModule,
        )
    }
}

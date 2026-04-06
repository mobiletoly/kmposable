package dev.goquick.kmposable.sampleapp

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.goquick.kmposable.navigation3.navKeySavedStateConfiguration
import dev.goquick.kmposable.navigation3.pushSingleTop
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsFlowEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data object ContactsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

internal val sampleAppNavConfiguration: SavedStateConfiguration = navKeySavedStateConfiguration(
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ContactsRoute::class, ContactsRoute.serializer())
            subclass(SettingsRoute::class, SettingsRoute.serializer())
        }
    }
)

internal class SampleAppNavigationController(
    private val backStack: MutableList<NavKey>,
) {
    fun onContactsOutput(output: ContactsFlowEvent) {
        if (output is ContactsFlowEvent.OpenSettings) {
            backStack.pushSingleTop(SettingsRoute)
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}

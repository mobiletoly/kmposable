package dev.goquick.kmposable.core.nav

internal actual fun stableIdentityHashCode(instance: Any): Int =
    System.identityHashCode(instance)

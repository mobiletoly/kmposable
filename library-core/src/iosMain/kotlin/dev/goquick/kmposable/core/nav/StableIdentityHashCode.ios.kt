package dev.goquick.kmposable.core.nav

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@OptIn(ExperimentalNativeApi::class)
internal actual fun stableIdentityHashCode(instance: Any): Int =
    instance.identityHashCode()

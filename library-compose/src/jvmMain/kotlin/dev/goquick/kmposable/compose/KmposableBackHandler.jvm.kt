package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.runtime.NavFlow

@Composable
actual fun <OUT : Any> KmposableBackHandler(runtime: NavFlow<OUT, *>) {
    // Desktop/JVM platforms don't have a system back concept; no-op for now.
}

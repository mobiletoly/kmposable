package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.runtime.NavFlow

@Composable
actual fun <OUT : Any> KmposableBackHandler(runtime: NavFlow<OUT, *>) {
    // No-op on iOS and other platforms
}

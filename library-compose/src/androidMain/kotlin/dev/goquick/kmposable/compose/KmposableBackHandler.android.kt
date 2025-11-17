package dev.goquick.kmposable.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goquick.kmposable.runtime.NavFlow

@Composable
actual fun <OUT : Any> KmposableBackHandler(runtime: NavFlow<OUT, *>) {
    val navState by runtime.navState.collectAsState()
    BackHandler(enabled = navState.size > 1) {
        runtime.pop()
    }
}

package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope

@Composable
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> rememberNavFlow(
    key: Any? = Unit,
    navFlowFactory: (CoroutineScope) -> NavFlow<OUT, ENTRY>,
): NavFlow<OUT, ENTRY> {
    val appScope = rememberCoroutineScope()
    val navFlow = remember(key, appScope) {
        navFlowFactory(appScope)
    }

    LaunchedEffect(navFlow) {
        navFlow.start()
    }
    DisposableEffect(navFlow) {
        onDispose { navFlow.dispose() }
    }

    return navFlow
}

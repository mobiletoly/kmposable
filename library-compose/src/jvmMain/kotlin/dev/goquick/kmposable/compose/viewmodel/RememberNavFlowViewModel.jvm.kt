package dev.goquick.kmposable.compose.viewmodel

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope

@Composable
actual fun <OUT : Any> rememberNavFlowViewModel(
    factory: (CoroutineScope) -> NavFlow<OUT, *>
): NavFlowViewModel<OUT> = navFlowViewModel<RememberNavFlowViewModel<OUT>, OUT> {
    createRememberNavFlowViewModel(factory)
}


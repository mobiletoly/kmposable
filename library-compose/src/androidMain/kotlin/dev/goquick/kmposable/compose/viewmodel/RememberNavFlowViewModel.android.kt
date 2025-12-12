package dev.goquick.kmposable.compose.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random

@Composable
actual fun <OUT : Any> rememberNavFlowViewModel(
    factory: (CoroutineScope) -> NavFlow<OUT, *>
): NavFlowViewModel<OUT> {
    val instanceKey = rememberSaveable { Random.nextLong() }
    val key = "kmposable.rememberNavFlowViewModel:$instanceKey"
    val viewModelFactory = InternalKmposableViewModelFactory {
        createRememberNavFlowViewModel(factory)
    }

    return viewModel<RememberNavFlowViewModel<OUT>>(
        key = key,
        factory = viewModelFactory,
    )
}

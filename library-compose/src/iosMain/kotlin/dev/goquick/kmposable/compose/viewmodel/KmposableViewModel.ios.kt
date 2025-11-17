package dev.goquick.kmposable.compose.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual inline fun <reified VM, OUT : Any> navFlowViewModel(
    noinline factory: () -> VM
): VM where VM : NavFlowViewModel<OUT> {
    return remember { factory() }
}

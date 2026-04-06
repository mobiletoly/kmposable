package dev.goquick.kmposable.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import dev.goquick.kmposable.compose.NavFlowHost
import dev.goquick.kmposable.compose.NodeRenderer
import dev.goquick.kmposable.compose.viewmodel.rememberNavFlowViewModel
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.modules.SerializersModule

@Composable
public fun <KEY : Any> rememberKmposableNavEntryDecorators(): List<NavEntryDecorator<KEY>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )

@Composable
public fun <OUT : Any> rememberNavigation3NavFlow(
    factory: (CoroutineScope) -> NavFlow<OUT, *>
): NavFlow<OUT, *> = rememberNavFlowViewModel(factory).navFlow

public fun <KEY : Any, OUT : Any> kmposableNavFlowEntry(
    key: KEY,
    navFlowFactory: (CoroutineScope) -> NavFlow<OUT, *>,
    renderer: NodeRenderer<OUT>,
    onOutput: suspend (OUT) -> Unit = {},
): NavEntry<KEY> =
    kmposableNavFlowEntry(
        key = key,
        navFlowFactory = navFlowFactory,
        onOutput = onOutput,
    ) { navFlow ->
        NavFlowHost(navFlow = navFlow, renderer = renderer)
    }

public fun <KEY : Any, OUT : Any> kmposableNavFlowEntry(
    key: KEY,
    navFlowFactory: (CoroutineScope) -> NavFlow<OUT, *>,
    onOutput: suspend (OUT) -> Unit = {},
    content: @Composable (NavFlow<OUT, *>) -> Unit,
): NavEntry<KEY> = NavEntry(key) {
    val navFlow = rememberNavigation3NavFlow(navFlowFactory)
    CollectNavFlowOutputs(navFlow = navFlow, onOutput = onOutput)
    content(navFlow)
}

@Composable
public fun <OUT : Any> CollectNavFlowOutputs(
    navFlow: NavFlow<OUT, *>,
    onOutput: suspend (OUT) -> Unit,
) {
    LaunchedEffect(navFlow, onOutput) {
        navFlow.outputs.collect { output -> onOutput(output) }
    }
}

public fun navKeySavedStateConfiguration(
    serializersModule: SerializersModule
): SavedStateConfiguration = SavedStateConfiguration {
    this.serializersModule = serializersModule
}

public fun <KEY : Any> MutableList<KEY>.pushSingleTop(destination: KEY): Boolean {
    if (lastOrNull() == destination) return false
    add(destination)
    return true
}

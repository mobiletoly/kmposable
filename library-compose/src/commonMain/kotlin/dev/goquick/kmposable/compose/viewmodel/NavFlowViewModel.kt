/*
 * Copyright 2025 Toly Pochkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.goquick.kmposable.compose.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.runtime.NavFlow
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.cancel

/**
 * Base [ViewModel] that owns a [NavFlow] instance and automatically
 * starts and disposes it in the ViewModel lifecycle.
 *
 * Use this class when your feature consists of a **flow** of one or more [Node]s
 * (e.g. a list → detail → edit sequence, tabbed navigation, wizard, etc.).
 * In that scenario you have a navigation stack, outputs that signal “next steps” or
 * “flow finished”, and you benefit from having the container manage the nodes.
 *
 * Example:
 * ```
 * class ContactsFlowViewModel : NavFlowViewModel<ContactsOutput>(
 *     NavFlow(appScope = viewModelScope, rootNode = ContactsListNode(viewModelScope))
 * )
 * ```
 *
 * If your screen logic is simpler and consists of a single [Node] (no navigation stack),
 * you may instead use [NodeViewModel] (via alias [ScreenNodeViewModel]) which wraps
 * just one [Node] and exposes its state and events.
 *
 * @param OUT the output type emitted when the feature flow completes or transitions to another
 * screen.
 */
abstract class NavFlowViewModel<OUT : Any>(
    val navFlow: NavFlow<OUT, *>
) : ViewModel() {

    init {
        navFlow.start()
    }

    override fun onCleared() {
        navFlow.dispose()
        super.onCleared()
    }
}

/**
 * Multiplatform entry point for acquiring [NavFlowViewModel] instances inside composables.
 * Android implementations integrate with `viewModel()`, while other platforms simply
 * remember the provided instance factory.
 */
@Composable
expect inline fun <reified VM, OUT : Any> navFlowViewModel(
    noinline factory: () -> VM
): VM where VM : NavFlowViewModel<OUT>

/**
 * Convenience wrapper around [navFlowViewModel] that can be reused across composables.
 */
class NavFlowViewModelProvider<VM, OUT : Any>(
    val factory: () -> VM
) where VM : NavFlowViewModel<OUT>

@Composable
inline fun <reified VM, reified OUT : Any> NavFlowViewModelProvider<VM, OUT>.get(): VM
    where VM : NavFlowViewModel<OUT> {
    return navFlowViewModel(factory = factory)
}

/**
 * ViewModel for a single [Node<STATE, EVENT, OUT>] that exposes [state] and [onEvent].
 *
 * Use this class when your screen logic is contained in one [Node] and you do *not* need
 * navigation-stack support or flow outputs.
 *
 * Example:
 * ```
 * class CounterScreenViewModel : ScreenNodeViewModel<CounterState, CounterEvent, Nothing>(
 *     CounterNode(scope)
 * )
 * ```
 *
 * @param NODE type of the node being wrapped
 * @param STATE the state type of the node
 * @param EVENT the event type of the node
 * @param OUT the output type of the node (often used for navigation or flow result)
 */
open class NodeViewModel<
    NODE,
    STATE : Any,
    EVENT : Any,
    OUT : Any
>(
    protected val node: NODE
) : ViewModel()
    where NODE : Node<STATE, EVENT, OUT> {

    val state: StateFlow<STATE> get() = node.state

    fun onEvent(event: EVENT) {
        node.onEvent(event)
    }
}

// Convenience alias for view models that wrap a single screen node.
typealias ScreenNodeViewModel<STATE, EVENT, OUT> =
    NodeViewModel<Node<STATE, EVENT, OUT>, STATE, EVENT, OUT>

fun interface KmposableViewModelFactory {
    fun create(modelClass: KClass<out ViewModel>): ViewModel
}

val LocalKmposableViewModelFactory =
    staticCompositionLocalOf<KmposableViewModelFactory?> { null }

@Composable
inline fun <reified VM, OUT : Any> kmposableViewModelFromDi(): VM
    where VM : NavFlowViewModel<OUT> {

    val factory = LocalKmposableViewModelFactory.current
        ?: error(
            "No KmposableViewModelFactory provided. " +
                "Supply LocalKmposableViewModelFactory in CompositionLocalProvider " +
                "or use kmposableViewModel { ... }."
        )

    return navFlowViewModel {
        @Suppress("UNCHECKED_CAST")
        factory.create(VM::class) as VM
    }
}

/**
 * Helper to create and remember a [NavFlowViewModel] that owns a [NavFlow] built from the provided
 * [factory]. The NavFlow survives configuration changes and is disposed when the ViewModel clears.
 */
@Composable
fun <OUT : Any> rememberNavFlowViewModel(
    factory: (kotlinx.coroutines.CoroutineScope) -> NavFlow<OUT, *>
): NavFlowViewModel<OUT> = navFlowViewModel {
    val flowScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
    )
    object : NavFlowViewModel<OUT>(factory(flowScope)) {
        override fun onCleared() {
            super.onCleared()
            flowScope.cancel()
        }
    }
}

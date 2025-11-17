package dev.goquick.kmposable.compose.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
actual inline fun <reified VM, OUT : Any> navFlowViewModel(
    noinline factory: () -> VM
): VM where VM : NavFlowViewModel<OUT> {
    val viewModelFactory = InternalKmposableViewModelFactory(factory)
    return viewModel(
        modelClass = VM::class,
        factory = viewModelFactory
    )
}

@PublishedApi
internal class InternalKmposableViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = creator()
        if (!modelClass.isInstance(viewModel)) {
            error("Factory created ${viewModel::class.java}, expected ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}

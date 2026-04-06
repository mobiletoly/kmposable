package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import dev.goquick.kmposable.core.nav.KmposableNavState
import dev.goquick.kmposable.core.nav.KmposableStackEntry
import dev.goquick.kmposable.core.nav.diffNavState

@Composable
internal fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> rememberNavFlowSaveableStateHolder(
    navState: KmposableNavState<OUT, ENTRY>
): SaveableStateHolder {
    val saveableStateHolder = rememberSaveableStateHolder()
    var previousState by remember { mutableStateOf<KmposableNavState<OUT, ENTRY>?>(null) }

    LaunchedEffect(navState) {
        val diff = diffNavState(previousState, navState)
        diff.popped.forEach { entry ->
            saveableStateHolder.removeState(entry.saveableStateKey)
        }
        previousState = navState
    }

    return saveableStateHolder
}

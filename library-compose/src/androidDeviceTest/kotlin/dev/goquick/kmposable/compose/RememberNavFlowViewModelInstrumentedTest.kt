package dev.goquick.kmposable.compose

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goquick.kmposable.compose.viewmodel.NavFlowViewModel
import dev.goquick.kmposable.compose.viewmodel.rememberNavFlowViewModel
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class RememberNavFlowViewModelInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun viewModelIsNotAnonymousAndSurvivesConfigurationChange() {
        val tracker = RememberViewModelTracker()

        composeRule.setContent {
            RememberFlowScreen(tracker)
        }

        composeRule.runOnIdle {
            val viewModel = tracker.require()
            assertFalse(viewModel::class.java.isAnonymousClass)
            assertFalse(viewModel::class.java.isLocalClass)

            viewModel.navFlow.push(RememberTestNode("Details"))
            tracker.topNodeBefore = viewModel.navFlow.navState.value.top
            assertEquals(2, viewModel.navFlow.navState.value.size)
        }

        composeRule.activityRule.scenario.recreate()

        composeRule.runOnIdle {
            val viewModel = tracker.require()
            assertSame(tracker.initial, viewModel)
            assertEquals(2, viewModel.navFlow.navState.value.size)

            val top = viewModel.navFlow.navState.value.top as RememberTestNode
            assertSame(tracker.topNodeBefore, top)
            assertEquals("Details", top.label)
        }
    }
}

@Composable
private fun RememberFlowScreen(tracker: RememberViewModelTracker) {
    val viewModel: NavFlowViewModel<RememberVmOutput> = rememberNavFlowViewModel { appScope ->
        RememberTestRuntime(
            appScope = appScope,
            root = RememberTestNode("Root"),
        )
    }
    LaunchedEffect(viewModel) {
        tracker.update(viewModel)
    }
    Text("ok")
}

private class RememberViewModelTracker {
    var initial: NavFlowViewModel<RememberVmOutput>? = null
    var current: NavFlowViewModel<RememberVmOutput>? = null
    var topNodeBefore: Node<*, *, *>? = null

    fun update(viewModel: NavFlowViewModel<RememberVmOutput>) {
        if (initial == null) initial = viewModel
        current = viewModel
    }

    fun require(): NavFlowViewModel<RememberVmOutput> =
        current ?: error("ViewModel not ready")
}

private class RememberTestRuntime(
    appScope: CoroutineScope,
    root: RememberTestNode,
) : NavFlow<RememberVmOutput, DefaultStackEntry<RememberVmOutput>>(
    appScope = appScope,
    rootNode = root
)

private class RememberTestNode(
    val label: String
) : Node<String, Unit, RememberVmOutput> {
    override val state = MutableStateFlow(label)
    override val outputs = MutableSharedFlow<RememberVmOutput>(extraBufferCapacity = 1)
    override fun onEvent(event: Unit) = Unit
}

private sealed interface RememberVmOutput

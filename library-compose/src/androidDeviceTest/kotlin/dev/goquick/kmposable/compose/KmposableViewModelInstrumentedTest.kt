package dev.goquick.kmposable.compose

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.goquick.kmposable.compose.viewmodel.NavFlowViewModel
import dev.goquick.kmposable.compose.viewmodel.navFlowViewModel
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.runtime.NavFlow
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class KmposableViewModelInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun runtimeSurvivesConfigurationChange() {
        val tracker = ViewModelTracker()

        composeRule.setContent {
            TestFlowScreen(tracker)
        }

        composeRule.runOnIdle {
            val viewModel = tracker.require()
            viewModel.navFlow.push(TestNode("Details"))
            tracker.topNodeBefore = viewModel.navFlow.navState.value.top
            assertEquals(2, viewModel.navFlow.navState.value.size)
        }

        composeRule.activityRule.scenario.recreate()

        composeRule.runOnIdle {
            val viewModel = tracker.require()
            assertSame(tracker.initial, viewModel)
            assertEquals(2, viewModel.navFlow.navState.value.size)
            val top = viewModel.navFlow.navState.value.top as TestNode
            assertSame(tracker.topNodeBefore, top)
            assertEquals("Details", top.label)
        }
    }
}

@Composable
private fun TestFlowScreen(tracker: ViewModelTracker) {
    val viewModel = navFlowViewModel {
        TestFlowViewModel(TestRuntime(TestNode("Root")))
    }
    LaunchedEffect(viewModel) {
        tracker.update(viewModel)
    }
    Text(viewModel.currentNodeLabel())
}

private class ViewModelTracker {
    var initial: TestFlowViewModel? = null
    var current: TestFlowViewModel? = null
    var topNodeBefore: Node<*, *, *>? = null

    fun update(viewModel: TestFlowViewModel) {
        if (initial == null) initial = viewModel
        current = viewModel
    }

    fun require(): TestFlowViewModel =
        current ?: error("ViewModel not ready")
}

private class TestFlowViewModel(
    runtime: TestRuntime
) : NavFlowViewModel<VmTestOutput>(runtime) {
    fun currentNodeLabel(): String {
        val node = navFlow.navState.value.top as TestNode
        return node.label
    }
}

private class TestRuntime(
    root: TestNode
) : NavFlow<VmTestOutput, DefaultStackEntry<VmTestOutput>>(
    appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    rootNode = root
)

private class TestNode(
    val label: String
) : Node<String, Unit, VmTestOutput> {
    override val state = MutableStateFlow(label)
    override val outputs = MutableSharedFlow<VmTestOutput>(extraBufferCapacity = 1)
    override fun onEvent(event: Unit) = Unit
}

private sealed interface VmTestOutput

package dev.goquick.kmposable.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goquick.kmposable.core.Node
import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.runtime.NavFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavFlowHostSaveableStateInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun restoresRememberSaveableStateWhenPoppingBackToPreviousEntry() {
        val runtime = SaveableStateTestRuntime()
        runtime.start()
        composeRule.setContent {
            val renderer = rememberSaveableCounterRenderer()
            NavFlowHost(navFlow = runtime, renderer = renderer)
        }

        composeRule.onNodeWithText("Root count: 0").assertExists()
        composeRule.onNodeWithText("Increment Root").performClick()
        composeRule.onNodeWithText("Root count: 1").assertExists()

        composeRule.runOnUiThread {
            runtime.push(SaveableCounterNode("Details"))
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Details count: 0").assertExists()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Root count: 1").assertExists()
        composeRule.onNodeWithText("Root count: 0").assertDoesNotExist()
    }

    @Test
    fun replaceAllDoesNotLeakSaveableStateIntoNewEntry() {
        val runtime = SaveableStateTestRuntime()
        runtime.start()
        composeRule.setContent {
            val renderer = rememberSaveableCounterRenderer()
            NavFlowHost(navFlow = runtime, renderer = renderer)
        }

        composeRule.onNodeWithText("Increment Root").performClick()
        composeRule.onNodeWithText("Root count: 1").assertExists()

        composeRule.runOnUiThread {
            runtime.replaceAll(SaveableCounterNode("Fresh"))
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Fresh count: 0").assertExists()
        composeRule.onNodeWithText("Fresh count: 1").assertDoesNotExist()
    }

    @Test
    fun replaceTopWithSameNodeInstanceResetsRememberSaveableState() {
        val sharedNode = SaveableCounterNode("Shared")
        val runtime = SaveableStateTestRuntime(rootNode = sharedNode)
        runtime.start()
        composeRule.setContent {
            val renderer = rememberSaveableCounterRenderer()
            NavFlowHost(navFlow = runtime, renderer = renderer)
        }

        composeRule.onNodeWithText("Increment Shared").performClick()
        composeRule.onNodeWithText("Shared count: 1").assertExists()

        composeRule.runOnUiThread {
            runtime.replaceTop(sharedNode)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Shared count: 0").assertExists()
        composeRule.onNodeWithText("Shared count: 1").assertDoesNotExist()
    }
}

@Composable
private fun rememberSaveableCounterRenderer(): NodeRenderer<SaveableStateTestOutput> {
    return remember {
        nodeRenderer {
            register<SaveableCounterNode> { node ->
                var count by rememberSaveable { mutableIntStateOf(0) }
                Column {
                    Text("${node.label} count: $count")
                    Button(onClick = { count++ }) {
                        Text("Increment ${node.label}")
                    }
                }
            }
        }
    }
}

private class SaveableStateTestRuntime(
    rootNode: SaveableCounterNode = SaveableCounterNode("Root")
) : NavFlow<SaveableStateTestOutput, DefaultStackEntry<SaveableStateTestOutput>>(
    appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    rootNode = rootNode,
)

private sealed interface SaveableStateTestOutput

private class SaveableCounterNode(
    val label: String,
) : Node<String, Unit, SaveableStateTestOutput> {
    private val _state = MutableStateFlow(label)
    override val state = _state.asStateFlow()
    private val _outputs = MutableSharedFlow<SaveableStateTestOutput>(extraBufferCapacity = 1)
    override val outputs = _outputs.asSharedFlow()
    override fun onEvent(event: Unit) = Unit
}

package dev.goquick.kmposable.compose

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class KmposableHostInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersTopNodeAndHandlesBack() {
        val runtime = TestRuntime()
        runtime.start()
        composeRule.setContent {
            val renderer = remember {
                nodeRenderer<AppTestOutput> {
                    register<TestTextNode> { node ->
                        val state = node.state.collectAsState().value
                        Text(state)
                    }
                }
            }
            NavFlowHost(navFlow = runtime, renderer = renderer)
        }

        composeRule.onNodeWithText("Root").assertExists()

        composeRule.runOnUiThread {
            runtime.push(TestTextNode("Details"))
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Details").assertExists()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Root").assertExists()
    }

    private class TestRuntime : NavFlow<AppTestOutput, DefaultStackEntry<AppTestOutput>>(
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        rootNode = TestTextNode("Root")
    )
}

private sealed interface AppTestOutput

private class TestTextNode(
    private val label: String
) : Node<String, Unit, AppTestOutput> {
    private val _state = MutableStateFlow(label)
    override val state = _state.asStateFlow()
    private val _outputs = MutableSharedFlow<AppTestOutput>(extraBufferCapacity = 1)
    override val outputs = _outputs.asSharedFlow()
    override fun onEvent(event: Unit) = Unit
}

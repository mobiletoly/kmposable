package dev.goquick.kmposable.navigation3

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.goquick.kmposable.core.LifecycleAwareNode
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
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class Navigation3NavFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun navFlowSurvivesWhileEntryRemainsAndDisposesWhenPopped() {
        val tracker = RouteFlowTracker()
        val backStack = mutableStateListOf(TestRoute.Root)

        composeRule.setContent {
            val entryDecorators = rememberKmposableNavEntryDecorators<TestRoute>()
            NavDisplay(
                backStack = backStack,
                entryDecorators = entryDecorators,
                entryProvider = { route ->
                    NavEntry(route) {
                        val navFlow = rememberNavigation3NavFlow<TestOutput> { scope ->
                            TrackingNavFlow(route, tracker, scope)
                        }
                        LaunchedEffect(navFlow) {
                            tracker.recordFlow(route, navFlow)
                        }
                        Text(route.label)
                    }
                },
            )
        }

        lateinit var rootFlow: NavFlow<TestOutput, *>
        composeRule.runOnIdle {
            rootFlow = tracker.requireFlow(TestRoute.Root)
            assertFalse(tracker.wasDisposed(TestRoute.Root))
        }

        composeRule.onNodeWithText(TestRoute.Root.label).assertExists()

        composeRule.runOnUiThread {
            backStack.add(TestRoute.Details)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestRoute.Details.label).assertExists()

        composeRule.runOnIdle {
            assertSame(rootFlow, tracker.requireFlow(TestRoute.Root))
            assertFalse(tracker.wasDisposed(TestRoute.Root))
            assertFalse(tracker.wasDisposed(TestRoute.Details))
        }

        composeRule.runOnUiThread {
            backStack.removeAt(backStack.lastIndex)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(TestRoute.Root.label).assertExists()

        composeRule.runOnIdle {
            assertSame(rootFlow, tracker.requireFlow(TestRoute.Root))
            assertFalse(tracker.wasDisposed(TestRoute.Root))
            assertTrue(tracker.wasDisposed(TestRoute.Details))
        }
    }
}

private enum class TestRoute(val label: String) {
    Root("Root"),
    Details("Details"),
}

private sealed interface TestOutput

private class TrackingNavFlow(
    route: TestRoute,
    tracker: RouteFlowTracker,
    appScope: CoroutineScope,
) : NavFlow<TestOutput, DefaultStackEntry<TestOutput>>(
    appScope = appScope,
    rootNode = TrackingNode(route, tracker),
)

private class TrackingNode(
    private val route: TestRoute,
    private val tracker: RouteFlowTracker,
) : Node<String, Unit, TestOutput>, LifecycleAwareNode {
    private val _state = MutableStateFlow(route.label)
    override val state = _state.asStateFlow()
    private val _outputs = MutableSharedFlow<TestOutput>(extraBufferCapacity = 1)
    override val outputs = _outputs.asSharedFlow()

    override fun onEvent(event: Unit) = Unit

    override fun onDetach() {
        tracker.recordDisposed(route)
    }
}

private class RouteFlowTracker {
    private val flows = mutableMapOf<TestRoute, NavFlow<TestOutput, *>>()
    private val disposedRoutes = mutableSetOf<TestRoute>()

    fun recordFlow(route: TestRoute, navFlow: NavFlow<TestOutput, *>) {
        flows[route] = navFlow
    }

    fun requireFlow(route: TestRoute): NavFlow<TestOutput, *> =
        flows[route] ?: error("No flow recorded for $route")

    fun recordDisposed(route: TestRoute) {
        disposedRoutes += route
    }

    fun wasDisposed(route: TestRoute): Boolean = route in disposedRoutes
}

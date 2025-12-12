package dev.goquick.kmposable.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RememberNavFlowViewModelCollisionInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun twoRememberNavFlowViewModelsDoNotCollideInSameOwner() {
        val tracker = CollisionTracker()

        composeRule.setContent {
            Column {
                FlowAScreen(tracker)
                FlowBScreen(tracker)
            }
        }

        composeRule.runOnIdle {
            val vmA = tracker.vmA ?: error("vmA missing")
            val vmB = tracker.vmB ?: error("vmB missing")

            // Regression: if both flows share the same underlying ViewModel key, Android will
            // return the same instance for both calls and the wrong NavFlow will be rendered.
            assertTrue(vmA !== vmB)
            assertTrue(vmA.navFlow.navState.value.root is NodeA)
            assertTrue(vmB.navFlow.navState.value.root is NodeB)
        }
    }
}

@Composable
private fun FlowAScreen(tracker: CollisionTracker) {
    val vm: NavFlowViewModel<CollisionOutputA> = rememberNavFlowViewModel { appScope ->
        CollisionNavFlowA(appScope = appScope)
    }
    LaunchedEffect(vm) { tracker.vmA = vm }
    Text("A")
}

@Composable
private fun FlowBScreen(tracker: CollisionTracker) {
    val vm: NavFlowViewModel<CollisionOutputB> = rememberNavFlowViewModel { appScope ->
        CollisionNavFlowB(appScope = appScope)
    }
    LaunchedEffect(vm) { tracker.vmB = vm }
    Text("B")
}

private class CollisionTracker {
    var vmA: NavFlowViewModel<CollisionOutputA>? = null
    var vmB: NavFlowViewModel<CollisionOutputB>? = null
}

private class CollisionNavFlowA(
    appScope: CoroutineScope,
) : NavFlow<CollisionOutputA, DefaultStackEntry<CollisionOutputA>>(
    appScope = appScope,
    rootNode = NodeA(),
)

private class CollisionNavFlowB(
    appScope: CoroutineScope,
) : NavFlow<CollisionOutputB, DefaultStackEntry<CollisionOutputB>>(
    appScope = appScope,
    rootNode = NodeB(),
)

private class NodeA : Node<String, Unit, CollisionOutputA> {
    override val state = MutableStateFlow("A")
    override val outputs = MutableSharedFlow<CollisionOutputA>(extraBufferCapacity = 1)
    override fun onEvent(event: Unit) = Unit
}

private class NodeB : Node<String, Unit, CollisionOutputB> {
    override val state = MutableStateFlow("B")
    override val outputs = MutableSharedFlow<CollisionOutputB>(extraBufferCapacity = 1)
    override fun onEvent(event: Unit) = Unit
}

private sealed interface CollisionOutputA
private sealed interface CollisionOutputB

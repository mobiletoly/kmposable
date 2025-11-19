---
layout: page
title: Hello Kmposable
permalink: /guides/hello-kmposable/
---

This walkthrough shows the smallest flow end-to-end. You’ll define a node, run it headlessly, render
it in Compose, and test it without UI.

## 1. Install Dependencies

```kotlin
dependencies {
    implementation("dev.goquick.kmposable:core:<version>")
    implementation("dev.goquick.kmposable:compose:<version>")
    testImplementation("dev.goquick.kmposable:test:<version>")
}
```

Replace `<version>` with the latest release from Maven Central.

## 2. Define a Node

```kotlin
data class CounterState(val value: Int = 0)
sealed interface CounterEvent { object Increment : CounterEvent; object Decrement : CounterEvent }

class CounterNode(parentScope: CoroutineScope) :
    StatefulNode<CounterState, CounterEvent, Nothing>(parentScope, CounterState()) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment -> updateState { it.copy(value = it.value + 1) }
            CounterEvent.Decrement -> updateState { it.copy(value = it.value - 1) }
        }
    }
}
```

## 3. Run a NavFlow

```kotlin
val navFlow = NavFlow(scope, CounterNode(scope)).apply { start() }
navFlow.sendEvent(CounterEvent.Increment)
println(navFlow.navState.value.top.state.value) // 1
```

## 4. Render with Compose

```kotlin
@Composable
fun CounterScreen() {
    val navFlow = rememberNavFlow { scope -> NavFlow(scope, CounterNode(scope)) }
    val renderer = remember {
        nodeRenderer<Nothing> {
            register<CounterNode> { node ->
                val state by node.state.collectAsState()
                CounterUi(state.value) { node.onEvent(CounterEvent.Increment) }
            }
        }
    }
    NavFlowHost(navFlow, renderer)
}
```

## 5. Test Headlessly

```kotlin
@Test
fun counterIncrements() = runTest {
    val factory = SimpleNavFlowFactory<Nothing> { NavFlow(this, CounterNode(this)) }

    factory.createTestScenario(this)
        .start()
        .send(CounterEvent.Increment)
        .apply {
            val top = navFlow.navState.value.top as CounterNode
            assertEquals(1, top.state.value)
        }
        .finish()
}
```

That’s it—you’ve built a flow that works without UI, renders in Compose, and is fully testable. Continue with the [Guides]({{ site.baseurl }}/guides/) to dive deeper.

# Kmposable

A **UI-agnostic navigation + flow engine** for Kotlin Multiplatform.  
Kmposable lets you structure your app as pure, testable **Nodes** (state + events + outputs).  
Compose UI becomes a thin rendering layer on top.

> **Build your entire app flow without UI.  
> Add UI later.  
> Test everything headlessly.**

---

# Why Kmposable?

Modern Compose apps often struggle with:

- Navigation tightly coupled to UI
- Bloated ViewModels
- Hard-to-test flows (list → details → edit → confirm…)
- Reusing flows across platforms or screens
- Business logic hiding inside UI code

**Kmposable fixes all of this** by giving you:

### ✔ Pure business logic (Nodes)

State + events + outputs. No UI code.

### ✔ A tiny NavFlow engine that manages navigation

Push, pop, replace — all headless.

### ✔ Full flow testing without UI

Use `FlowTestScenario` to test deep navigation and logic headlessly.

### ✔ Clean UI adapters

Compose just *observes* state from your nodes.

# The Mental Model (20 Seconds)

You only need three concepts:

### **1. Node**

A screen/feature logic unit:  
`state + events + outputs`.

### **2. Navigator**

Manages a stack of nodes.

### **3. NavFlow**

Runs the flow, drives navigation, exposes state.

```
Node (state, events, outputs)
        |
        | outputs
        v
Navigator <---- NavFlow ----> UI Renderer
```

That’s the core of Kmposable.

---

# Installation

```kotlin
dependencies {
    implementation("dev.goquick.kmposable:core:0.2.0")
    implementation("dev.goquick.kmposable:compose:0.2.0") // optional UI layer
}
```


---

# Quick Start (Headless)

You can build and test your whole app without any UI.

### 1. Define a Node

```kotlin
data class CounterState(val value: Int = 0)

sealed interface CounterEvent {
    object Increment : CounterEvent
    object Decrement : CounterEvent
}

class CounterNode(parent: CoroutineScope) :
    StatefulNode<CounterState, CounterEvent, Nothing>( // Nothing = this node never emits outputs
        parentScope = parent,
        initialState = CounterState()
    ) {

    override fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment ->
                updateState { it.copy(value = it.value + 1) }
            CounterEvent.Decrement ->
                updateState { it.copy(value = it.value - 1) }
        }
    }
}
```

Here the third generic type parameter of `StatefulNode<STATE, EVENT, OUTPUT>` is `Nothing`, which in
Kotlin means "this node does not produce any outputs". If your node needs to emit outputs (for
navigation or side-effects), replace `Nothing` with your own sealed interface, for example
`CounterOutput`.

### 2. Create a navigation flow

```kotlin
val navFlow = NavFlow(
    appScope = scope,
    rootNode = CounterNode(scope)
)

navFlow.start()
navFlow.sendEvent(CounterEvent.Increment)

println(navFlow.navState.value.stack.last().state.value) // 1
```

---

# Testing a Flow

```kotlin
@Test
fun counterIncrements() = runTest {
    val factory = SimpleNavFlowFactory<Nothing> {
        NavFlow(
            appScope = this,
            rootNode = CounterNode(this)
        )
    }

    val scenario = factory.createTestScenario(this)
    scenario
        .start()
        .send(CounterEvent.Increment)
        .assertTopNodeIs<CounterNode>()
        .apply {
            val top = scenario.runtime.navState.value.top as CounterNode
            check(top.state.value == 1) { "Expected state.value == 1, was ${top.state.value}" }
        }
        .finish()
}
```

No Compose. No emulator. No UI.  
Just pure logic tested in milliseconds.

---

# Adding UI (Compose Multiplatform)

Kmposable provides a tiny Compose adapter.  
Compose observes state; Kmposable handles logic + navigation (including system back by default).

```kotlin
@Composable
fun CounterScreen() {
    val navFlow = rememberNavFlow(key = Unit) {
        NavFlow(
            appScope = it,
            rootNode = CounterNode(it)
        )
    }

    CounterUi(navFlow)
}

@Composable
private fun CounterUi(navFlow: NavFlow<Nothing, *>) {
    val state by navFlow.collectNodeState<CounterNode>()

    Column {
        Text("Count: ${state.value}")
        Button(onClick = { navFlow.sendEvent(CounterEvent.Increment) }) {
            Text("Increment")
        }
    }
}
```

### Optional: Renderer-based UI

For more complex or multi-node flows, use a renderer to map each `Node` type to a Composable.
Define it via the DSL and keep it stable with `remember`:

```kotlin
val renderer = remember {
    nodeRenderer<ContactsOutput> {
        register<ContactsListNode> { node ->
            val state by node.state.collectAsState()
            ContactsListScreen(state = state, onEvent = node::onEvent)
        }
        register<ContactDetailsNode> { node ->
            val state by node.state.collectAsState()
            ContactDetailsScreen(state = state, onEvent = node::onEvent)
        }
    }
}

NavFlowHost(navFlow = navFlow, renderer = renderer)
```

`nodeRenderer { ... }` declares the mapping; `remember { ... }` ensures the renderer is created
once per flow instance.

# ViewModel Integration

Kmposable works with Kotlin Multiplatform ViewModel if you choose to use it,
but ViewModels are completely optional.
All flows can run directly via NavFlow.

A common pattern is to let the ViewModel own the `NavFlow` and expose it to the UI:

```kotlin
class CounterFlowViewModel : NavFlowViewModel<Nothing>(
    NavFlow(
        appScope = viewModelScope,
        rootNode = CounterNode(viewModelScope)
    )
)
}

@Composable
fun CounterScreen() {
    val vm: CounterFlowViewModel = navFlowViewModel { CounterFlowViewModel() }

    CounterUi(vm.navFlow)
}
```

---

# NavHost Integration (Compose Multiplatform UI)

Kmposable plays nicely with `androidx.navigation.compose.NavHost` when you want NavHost to own
**top-level app routing** (tabs, drawers, deep links) and Kmposable to handle **feature flows**.

Below we model two destinations:

- **Contacts** – a multi-screen flow (list → details/edit) backed by a `NavFlow`.
- **Settings** – a simple single-screen feature rendered by a `Node` directly (no NavFlow).

## Root NavHost

```kotlin
@Composable
fun RootNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "contacts") {
        composable("contacts") {
            ContactsDestination(navController)
        }
        composable("settings") {
            SettingsDestination()
        }
    }
}
```

## Flow destination: Contacts (uses NavFlow)

```kotlin
@Composable
fun ContactsDestination(navController: NavHostController) {
    val navFlow = rememberNavFlow<ContactsOutput>(key = Unit) { scope ->
        NavFlow(
            appScope = scope,
            rootNode = ContactsListNode(scope)
        )
    }

    LaunchedEffect(navFlow) {
        navFlow.outputs.collect { output ->
            when (output) {
                is ContactsOutput.OpenDetails ->
                    navController.navigate("details/${output.contactId}")
                ContactsOutput.Exit ->
                    navController.popBackStack()
            }
        }
    }

    val renderer = remember {
        nodeRenderer<ContactsOutput> {
            register<ContactsListNode> { node ->
                val state by node.state.collectAsState()
                ContactsListScreen(state = state, onEvent = node::onEvent)
            }
            register<ContactDetailsNode> { node ->
                val state by node.state.collectAsState()
                ContactDetailsScreen(state = state, onEvent = node::onEvent)
            }
            register<EditContactNode> { node ->
                val state by node.state.collectAsState()
                EditContactScreen(state = state, onEvent = node::onEvent)
            }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

## Single-screen destination: Settings (no NavFlow)

For features that are just one screen, you don’t need `NavFlow` at all. Create the `Node`
directly and bind it to Compose:

```kotlin
@Composable
fun SettingsDestination() {
    val scope = rememberCoroutineScope()
    val node = remember { SettingsNode(scope) }
    val state by node.state.collectAsState()

    SettingsScreen(
        state = state,
        onEvent = node::onEvent,
    )
}
```

This keeps simple destinations headless and testable, while flows with internal navigation
use NavFlow without introducing extra NavFlow or ViewModel layers.

### Patterns to keep in mind

- **NavHost** owns app-level destinations (tabs, main sections, deep links).
- **NavFlow** owns inner feature flows (list/details/edit/etc.).
- Single-screen features can use a plain `Node` (optionally wrapped in
  `ScreenNodeViewModel`) without NavFlow.
- Node outputs typically map to NavHost navigation (`navigate`, `popBackStack`).

---

**Kmposable, by design, makes your app architecture clean:  
your logic lives in Nodes, your UI is optional, and your flows are fully testable.**  
Happy building!

---

# Samples

- `sample-app-compose` — full Compose Multiplatform app with NavHost + tabs

---

# 🤖 Kmposable is AI-friendly by design

Because Kmposable keeps logic headless and UI-agnostic, it’s easy for AI tools to
reason about your app. Nodes have explicit `STATE`, `EVENT`, and `OUTPUT`,
navigation is expressed as simple stack operations, and the entire flow can run
without any UI.

In practice this means an AI (or any automation tool) can:

- traverse your flows by sending events into a `NavFlow` and inspecting `navState`
- explore and test flows headlessly using `FlowTestScenario`
- visualize navigation graphs directly from the Node/stack structure
- safely generate or refactor Nodes because inputs/outputs are strongly typed
- scaffold UIs from your state models, keeping logic untouched

Most of the time the AI never needs to touch Compose code at all — it works against
pure Kotlin that describes your app’s behavior, and you decide how to render it.

---

# NavFlow Script API Spec (Sequential Flow Orchestration) – v2

## Status

- Stage: Design / Implementation-ready
- Module: `library-core` (pure Kotlin, no Compose)
- Breaking changes: **Not required**

## 1. Problem & Goals

Kmposable already gives us:
- `Node<STATE, EVENT, OUT>` – a headless, testable unit of behavior
- `NavFlow<OUT, ENTRY>` – a stack-based navigation flow of Nodes
- `NavFlowHost` / `library-compose` – a bridge to Compose UI
- `FlowTestScenario` – a way to drive flows in tests

**Missing piece:** A way to write **sequential, "story-like" orchestration** on top of `NavFlow`, in pure Kotlin, without touching Compose.

### Goals
1. Provide a sequential, coroutine-based orchestration API for NavFlow
2. Keep it headless and UI-agnostic (no Compose, no Android lifecycle)
3. Make it easy to use from production code (ViewModel) and tests (FlowTestScenario)
4. Keep it small and simple – minimal API surface, no new framework

### Non-Goals
- Not replacing NavFlow or existing output-listener patterns
- Not introducing a new UI rendering abstraction
- Not coupling scripts to Compose APIs or resources
- Not building a full-blown workflow engine

---

## 2. High-Level Concept

Example script:

```kotlin
fun NavFlow<ContactsFlowEvent, *>.launchContactsScript(scope: CoroutineScope): Job =
    launchNavFlowScript(scope) {
        // 1) Show splash
        showRoot { ContactsSplashNode(appScope) }
        delay(1500)

        // 2) Show sign-in form and wait for the outcome
        showRoot { SignInNode(appScope) }
        when (val action = awaitOutputOfType<ContactsFlowEvent.SignInAction>()) {
            ContactsFlowEvent.SignInAction.SignIn -> {
                showRoot { LoadingNode(appScope) }
                val result = signInRepository.signIn()
                if (result.isError) {
                    showRoot { ErrorNode(appScope, result.error) }
                } else {
                    showRoot { DashboardNode(appScope) }
                }
            }
            ContactsFlowEvent.SignInAction.SignUp -> {
                showRoot { SignUpNode(appScope) }
                awaitOutputOfType<ContactsFlowEvent.SignUpCompleted>()
                showRoot { DashboardNode(appScope) }
            }
        }
    }
```

**Key ideas:**
- A script is a suspend lambda that runs in a coroutine scope
- It manipulates the same NavFlow that the UI is rendering
- It can change the stack (via Navigator) and wait for outputs (via awaitOutput)
- No Compose calls or UI logic inside scripts

---

## 3. API Surface

### 3.1. NavFlowScriptScope

```kotlin
interface NavFlowScriptScope<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    /** The NavFlow being orchestrated. */
    val navFlow: NavFlow<OUT, ENTRY>

    /** The underlying navigator for stack operations. */
    val navigator: KmposableNavigator<OUT, ENTRY>

    /** Convenience entrypoint for stack operations. */
    fun showNode(block: KmposableNavigator<OUT, ENTRY>.() -> Unit)

    fun showRoot(factory: () -> Node<*, *, OUT>)
    fun pushNode(factory: () -> Node<*, *, OUT>)
    fun replaceTop(factory: () -> Node<*, *, OUT>)

    /** Optional tracing hook for debugging. */
    fun trace(message: () -> String)

    /**
     * Suspend until the next output that matches [predicate] is emitted.
     * 
     * Semantics:
     * - Only outputs emitted *after* this call starts collecting are considered
     * - Does *not* consume outputs for other collectors (uses NavFlow.outputs as broadcast)
     * - Respects coroutine cancellation
     */
    suspend fun <T : OUT> awaitOutput(predicate: (OUT) -> Boolean): T

    /**
     * Convenience: suspend until the next output of type [T] is emitted.
     * 
     * - Uses awaitOutput { it is T } under the hood
     * - If no such output is emitted before cancellation, propagates CancellationException
     * - No built-in timeout; callers can wrap with withTimeout if needed
     */
    suspend inline fun <reified T : OUT> awaitOutputOfType(): T =
        awaitOutput { it is T } as T
}
```

**Implementation notes:**
- `awaitOutput` should collect from `navFlow.outputs` (existing Flow)
- Should not use a separate Channel that consumes outputs
- Should only see outputs emitted after the call begins (no replay)

### 3.2. launchNavFlowScript

```kotlin
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlow<OUT, ENTRY>.launchNavFlowScript(
    scope: CoroutineScope,
    script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job
```

**Behavior:**
- Creates a NavFlowScriptScope bound to this NavFlow
- Launches the script in the provided CoroutineScope
- The script can call `showRoot`, `pushNode`, `replaceTop`, `showNode { }`, or use `navigator`
  directly to manipulate the stack
- The script can call `awaitOutput { }` / `awaitOutputOfType<T>()` to wait for outputs
- Optional `onTrace` callback receives `trace { … }` messages for logging

**Cancellation & lifecycle:**
- If scope is cancelled (e.g., ViewModel cleared), the script Job is cancelled
- `awaitOutput` / `awaitOutputOfType` must propagate CancellationException as usual
- If the script throws, the Job completes exceptionally; NavFlow is not automatically reset

**Single-script assumption:**
- We design assuming at most one script per NavFlow at a time
- Multiple concurrent scripts are considered undefined behavior for now
- No runtime enforcement required in v1

---

## 4. Output Wiring

`NavFlowScriptScope.awaitOutput` must tie into existing NavFlow output plumbing.

**Assumptions:**
- `NavFlow<OUT, ENTRY>` exposes outputs via a `Flow<OUT>` (e.g., SharedFlow<OUT>)
- That Flow is broadcast (multiple collectors allowed)

**High-level behavior:**
- `awaitOutput(predicate)`:
  - Collects from `navFlow.outputs`
  - Filters with predicate
  - Returns the first match
  - Ignores outputs emitted before collection starts (no special replay behavior)
  - Cancels cleanly when the script scope is cancelled
- `awaitOutputOfType<T>()`:
  - Uses `awaitOutput { it is T } as T`

**Timeouts** are the responsibility of callers (`withTimeout`, etc.), not part of this API.

---

## 5. Script Semantics & Rules

### 5.1. Single orchestrator per NavFlow
- We assume one script per NavFlow
- Multiple scripts on the same flow are undefined behavior (document this)
- No internal tracking of "active script" required at this stage

### 5.2. Cancellation & disposal
- `launchNavFlowScript` uses the provided CoroutineScope
- When the scope is cancelled, the script ends
- `awaitOutput` / `awaitOutputOfType` must be cancellation-cooperative
- If NavFlow has a disposal mechanism, callers coordinate scope cancellation with it

### 5.3. UI independence

**Scripts must not depend on UI concepts:**
- No Compose APIs
- No Android lifecycle APIs
- No direct calls to composables

**Scripts may use:**
- NavFlow / Navigator
- Domain services (repositories, use cases, etc.)
- Coroutines (delay, withTimeout, etc.)

---

## 6. Testing & FlowTestScenario Integration

FlowTestScenario already provides headless testing for flows.

**Should:**
- Ensure scripts can be run in tests using a CoroutineScope available in FlowTestScenario
- Optionally add a helper:

```kotlin
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> FlowTestScenario<OUT, ENTRY>.launchScript(
    script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job = runtime.launchNavFlowScript(scope, script)
```

**Example test:**

```kotlin
@Test
fun login_success_navigates_to_dashboard() = runTest {
    val scenario = FlowTestScenario(createLoginNavFlow(), this)

    val job = scenario.launchScript {
        runLoginScriptForTest()
    }

    // Drive events into nodes as needed,
    // or assert on navState snapshots while script runs.
}
```

This keeps scripts reusable between production and tests.

---

## 7. Compose Integration

No new APIs are required in library-compose for this feature.

**Usage pattern:**

1. Create NavFlow in a ViewModel:

```kotlin
class ContactsFlowViewModel(
    val navFlow: NavFlow<ContactsFlowEvent, *>,
    private val signInRepository: SignInRepository
) : ViewModel() {

    private val scriptJob = navFlow.launchNavFlowScript(viewModelScope) {
        runContactsFlowScript()
    }

    private suspend fun NavFlowScriptScope<ContactsFlowEvent, *>.runContactsFlowScript() {
        // orchestrate login/onboarding, etc.
    }

    override fun onCleared() {
        scriptJob.cancel()
        super.onCleared()
    }
}
```

2. Render with NavFlowHost in Compose:

```kotlin
@Composable
fun ContactsRootScreen(viewModel: ContactsFlowViewModel) {
    val navFlow = viewModel.navFlow
    val renderer = remember {
        nodeRenderer<ContactsFlowEvent> {
            // register nodes → composables
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

Compose is unaware of the script – it just reacts to changes in navState.

---

## 8. Summary

This feature adds:
- `NavFlowScriptScope<OUT, ENTRY>`
- `launchNavFlowScript(scope, script)` extension on NavFlow
- Helpers: `showNode`, `awaitOutput`, `awaitOutputOfType`

It enables writing sequential, coroutine-based orchestration on top of NavFlow, without touching Compose, while staying:
- Headless and testable
- KMP-friendly
- AI-friendly (flows described as linear scripts)
- Fully compatible with the existing NavFlow + Node + renderer architecture

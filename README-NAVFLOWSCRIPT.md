# NavFlow Scripts

Sequential orchestration for Kmposable flows lives entirely in Kotlin, without
touching Compose. NavFlow scripts give you a coroutine scope that can:

1. Mutate the NavFlow stack (`showNode { … }`)
2. Await node outputs (`awaitOutput { … }`, `awaitOutputOfType<T>()`)
3. Run in ViewModels, headless services, or tests (FlowTestScenario)

They keep your flow logic “story-like” while Compose continues to just render
whatever NavFlow exposes.

Looking for ready-to-use patterns? Check `docs/NAVFLOW_SCRIPT_COOKBOOK.md` for
recipes covering common flows (onboarding, list/details/edit, retries, etc.).

---

## When to use scripts

- **Onboarding / auth “stories”** (splash → login → loading → dashboard)
- **Wizard-style flows** (step1 → step2 → confirm)
- **Remote workflows** where UI simply mirrors the script’s current node
- **Headless automation/testing**: the same script feeds FlowTestScenario

Not needed for simple “ViewModel reacts to outputs” cases; scripts shine when
you’d otherwise write a large output listener with branching logic.

---

## API Overview

```kotlin
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> NavFlow<OUT, ENTRY>.launchNavFlowScript(
    scope: CoroutineScope,
    script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job
```

Inside the script:

```kotlin
interface NavFlowScriptScope<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    val navFlow: NavFlow<OUT, ENTRY>
    val navigator: KmposableNavigator<OUT, ENTRY>

    fun showNode(block: KmposableNavigator<OUT, ENTRY>.() -> Unit)
    fun showRoot(factory: () -> Node<*, *, OUT>)
    fun pushNode(factory: () -> Node<*, *, OUT>)
    fun replaceTop(factory: () -> Node<*, *, OUT>)
    fun trace(message: () -> String)
    suspend fun <T : OUT> awaitOutput(predicate: (OUT) -> Boolean): T
}

suspend inline fun <reified T : Any> NavFlowScriptScope<*, *>.awaitOutputOfType(): T
```

`showNode` funnels back through NavFlow so lifecycle hooks (onAttach/onDetach)
stay correct even when scripts replace nodes rapidly. The shortcuts `showRoot`,
`pushNode`, and `replaceTop` cover the common navigation verbs without exposing
entry creation to the script author. If you need instrumentation, call
`trace { "message" }` and pass a logger via `launchNavFlowScript(onTrace = …)`.

When scripts need to run repository work and reflect loading/error states on a
node, use the `runCatchingNodeCall` helper:

```kotlin
runCatchingNodeCall(
    onLoading = node::showLoading,
    onSuccess = node::showData,
    onError = { node.showError(it.message ?: "Unable to load") }
) {
    repository.fetch()
}
```

It standardises “show loading → perform suspending work → update success/error”
so individual scripts stay focused on orchestration.

Another handy helper is `awaitMappedOutput`, which loops until the mapper
returns a non-null value:

```kotlin
val action = awaitMappedOutput { event ->
    when (event) {
        is WizardOutput.StepCompleted -> StepAction.Proceed
        is WizardOutput.Cancelled -> StepAction.Cancel
        else -> null
    }
}
```

This keeps “wait for one of these outputs” logic tidy without manual
`when/else` scaffolding in every script.

Prefer a typed DSL? `awaitOutputCase` wraps the same idea with `on<T>` branches:

```kotlin
val listAction = awaitOutputCase<ListAction> {
    on<ContactsFlowEvent.OpenContact> { ListAction.Open(it.id) }
    on<ContactsFlowEvent.CreateContact> { ListAction.Create }
}
```

Unhandled outputs are ignored until one of the branches matches, so you don’t
need `else -> Unit` catch-alls anymore.

Need a quick way to push a node, run a block, and pop afterwards? Use `withNode`:

```kotlin
withNode(ContactDetailsNode(id, scope)) {
    showLoading()
    val contact = repository.getById(id) ?: return@withNode
    showContact(contact)
    // await actions, emit outputs, etc.
}
```

There’s also an overload that takes a factory lambda if you prefer:

```kotlin
withNode(factory = { ContactDetailsNode(id, scope) }) {
    // same as above
}

Need to push a node, wait for a specific result, then automatically pop it?
Use `pushForResult`:

```kotlin
val editorResult = pushForResult(
    factory = { EditContactNode(existingContact, scope) },
    mapper = { output ->
        when (output) {
            is ContactsFlowEvent.ContactSaved -> EditorResult.Saved(output.contact)
            ContactsFlowEvent.EditorCancelled -> EditorResult.Cancelled
            else -> null
        }
    }
)
```

It covers the common “push → run sub-flow → pop” navigation macro while keeping
the mapper strongly typed.
```

---

## ViewModel Usage Example

```kotlin
class OnboardingViewModel(
    private val repository: AuthRepository,
    val navFlow: NavFlow<OnboardingOutput, *>
) : ViewModel() {

    private val scriptJob = navFlow.launchNavFlowScript(viewModelScope) {
        showRoot { SplashNode(viewModelScope) }

        awaitOutputOfType<OnboardingOutput.SplashFinished>()

        showRoot { SignInNode(viewModelScope) }
        when (awaitOutputOfType<OnboardingOutput.AuthResult>()) {
            OnboardingOutput.AuthResult.Success ->
                showRoot { DashboardNode(viewModelScope) }
            is OnboardingOutput.AuthResult.Error ->
                showRoot { ErrorNode(viewModelScope) }
        }
    }

    override fun onCleared() {
        scriptJob.cancel()
        super.onCleared()
    }
}
```

Compose simply hosts the NavFlow:

```kotlin
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val renderer = remember {
        nodeRenderer<OnboardingOutput> {
            register<SplashNode> { node -> SplashUi(node) }
            register<SignInNode> { node -> SignInUi(node) }
            register<ErrorNode> { node -> ErrorUi(node) }
            register<DashboardNode> { node -> DashboardUi(node) }
        }
    }

    NavFlowHost(navFlow = viewModel.navFlow, renderer = renderer)
}
```

---

## Testing Scripts

`FlowTestScenario` has helpers:

```kotlin
fun <OUT : Any, ENTRY : KmposableStackEntry<OUT>> FlowTestScenario<OUT, ENTRY>.launchScript(
    onTrace: ((String) -> Unit)? = null,
    script: suspend NavFlowScriptScope<OUT, ENTRY>.() -> Unit
): Job

suspend fun FlowTestScenario<*, *>.awaitOutputOfType<T : Any>(): T

suspend fun FlowTestScenario<*, *>.awaitStackTags(vararg tags: String?)
```

Usage:

```kotlin
@Test
fun onboarding_script_reaches_dashboard() = runTest {
    val scenario = onboardingFlowFactory.createTestScenario(this).start()

    val job = scenario.launchScript { runOnboardingScript() }

    // Drive node events if needed…
    scenario.runtime.sendEvent(AuthEvent.Submit(credentials))

    job.join()
    scenario.assertTopNodeIs<DashboardNode>()
}
```

Because scripts are headless, no Compose or platform setup is required for
testing – just coroutines. Pass `onTrace` if you want to assert on script logs,
or use helpers like `awaitOutputOfType`, `awaitStackSize`, and `awaitStackTags`
to wait for specific navigation states.

> Want a concrete implementation? Check `sample-app-flowscript` in this repo.
> It runs the same contacts UI as the Compose sample, but all navigation is
> driven by a NavFlow script.

---

## Behavior Notes

- **Single script per NavFlow**: scripts assume exclusive orchestration. Running
  multiple scripts concurrently is undefined.
- **Cancellation**: cancelling the parent scope cancels `awaitOutput` calls and
  terminates the script; clean up your NavFlow in `onCleared`/`finish`.
- **No automatic timeouts**: wrap `awaitOutput` with `withTimeout` if you need
  fail-fast behavior.
- **UI-agnostic**: scripts belong in `library-core` (or your shared logic);
  Compose just observes the resulting navigation state.

---

## Checklist When Writing Scripts

1. Start your NavFlow before launching scripts (`navFlow.start()` or
   `rememberNavFlow`).
2. Launch scripts in the same scope that owns navFlow lifecycle
   (ViewModel scope, service scope, FlowTestScenario scope).
3. Use `showNode { … }` instead of mutating the navigator manually.
4. Prefer sealed output types so your `when` branches stay exhaustive.
5. Cancel the script job when the owning scope is destroyed.

Scripts give you a declarative, readable way to describe complex flows in pure
Kotlin while retaining Kmposable’s headless-first architecture. Use them
whenever “events + outputs” starts to feel like writing a finite-state machine
by hand; scripts are that state machine, as code.

---

## Reactive vs Sequential – how they fit together

Scripts do not replace the reactive model; they sit on top of it. Use this
mental model when deciding where each belongs.

### Why sequential scripts shine

1. **Multi-step “stories”** – Wizards, onboarding, checkout, KYC: these flows
   naturally read as “do A → wait → do B → branch”. Encoding them as
   `awaitOutputOfType` calls mirrors that narrative and keeps the branching
   rules in one place instead of scattering them across node handlers.
2. **Centralized policy** – Retry rules, success/failure branching,
   transitions after network calls all live in a single script. Reviews and
   diffs become straightforward.
3. **Testing & AI friendliness** – A coroutine that calls `runContactsFlowScript`
   can be executed headlessly: inject fake repositories, assert on state,
   or let an AI tool trace the story without understanding every node.
4. **Error handling** – Sequential code makes structured retries and fallback
   states easy (`repeat { signIn() }` loops, etc.).

### Where reactive nodes remain the right tool

1. **Local UI state** – Validation, input fields, Compose state observation,
   “enable button when form valid” logic is still reactive by nature.
2. **Ongoing streams** – Live telemetry, chat feeds, push updates don’t fit a
   one-shot script; keep those as Flows exposed by nodes.
3. **Parallel UI surfaces** – If parts of the UI are independent (dashboard
   cards, sidebars), scripting every permutation becomes cumbersome. Keep those
   reactive and let scripts orchestrate only the sub-flows that truly read like
   a story.

### Putting it together for Kmposable

- **Reactive layer (always there)**: Nodes expose StateFlow state, handle
  events, and emit outputs. NavFlow manages the stack. Compose observes state.
- **Sequential layer (optional)**: Scripts orchestrate “what happens after
  which output” for flows that have a clear narrative.

Use scripts when a flow genuinely benefits from being described linearly.
Stick with the reactive-only approach for simple or highly dynamic flows,
or mix both: nodes remain reactive locally, scripts coordinate the bigger
story beats.

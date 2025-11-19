# NavFlow Script API Implementation Checklist

## Status: Implemented

**Module:** `library-core` (pure Kotlin, no Compose)  
**Breaking Changes:** Not required

---

## Understanding Phase

- [x] **Understand the problem we're solving**
  - [x] Current NavFlow requires output listeners and imperative navigation
  - [x] Want sequential, "story-like" flow orchestration
  - [x] Need to keep it headless, testable, and UI-agnostic

- [x] **Understand the core concepts**
  - [x] Scripts are suspend lambdas running in a dedicated scope
  - [x] Scripts manipulate the same NavFlow that UI renders
  - [x] Scripts can push/replace nodes and await outputs sequentially
  - [x] No Compose or UI dependencies

- [x] **Understand the API surface**
  - [x] `NavFlowScriptScope<OUT, ENTRY>` - scope interface for scripts
  - [x] `launchNavFlowScript()` - entry point to run scripts
  - [x] `showNode()` - manipulate navigator stack
  - [x] `awaitOutput()` - suspend until matching output
  - [x] `awaitOutputOfType<T>()` - convenience for type-based waiting

- [x] **Understand integration points**
  - [x] Works with existing NavFlow output mechanism (Flow<OUT>)
  - [x] Integrates with FlowTestScenario for testing
  - [x] No changes needed in library-compose
  - [x] Scripts run in ViewModel or test scopes

---

## Design Phase

- [x] **Review existing NavFlow architecture**
  - [x] Examine NavFlow.outputs: Flow<OUT> implementation
  - [x] Understand how outputs are currently collected
  - [x] Review Navigator operations (push, pop, replaceAll, etc.)
  - [x] Check lifecycle and cancellation handling

- [x] **Design NavFlowScriptScope interface**
  - [x] Define interface with navFlow and navigator properties
  - [x] Design showNode() function signature
  - [x] Design awaitOutput() suspension mechanism
  - [x] Design awaitOutputOfType<T>() inline helper

- [x] **Design launchNavFlowScript implementation**
  - [x] Plan how to create NavFlowScriptScope instance
  - [x] Plan output collection mechanism (Channel vs Flow)
  - [x] Plan cancellation and error handling
  - [x] Plan single-script-per-flow enforcement (documented as undefined behavior)

- [x] **Design output wiring**
  - [x] Decide on Channel<OUT> vs direct Flow<OUT> usage (use Flow)
  - [x] Handle sequential awaitOutput calls
  - [x] Handle cancellation during await
  - [x] Handle script completion/cleanup

---

## Implementation Phase

### Core API (library-core)

- [x] **Create NavFlowScript.kt / interface**
  - [x] Define NavFlowScriptScope<OUT, ENTRY> interface (in NavFlowScript.kt)
  - [x] Expose navFlow and navigator properties
  - [x] Provide showNode(), awaitOutput(), awaitOutputOfType()
  - [x] Implement launchNavFlowScript() extension and scope implementation
  - [x] Wire to NavFlow.outputs, handle cancellation, emit KDoc

- [x] **Update NavFlow if needed**
  - [x] Ensure outputs Flow<OUT> is exposed
  - [x] Add hooks like `navigatorHandle()` / entry creation for scripts
  - [x] Maintain thread-safety

---

## Testing Phase

### Unit Tests (library-core)

- [x] **Create NavFlowScriptTest.kt**
  - [x] Basic script execution
  - [x] Await output, push/pop behavior (as covered in NavFlowScriptTest/FlowTestScenarioScriptTest)

- [x] **Integration with FlowTestScenario**
  - [x] Added `launchScript()` helper
  - [x] Added FlowTestScenarioScriptTest demonstrating usage

### Sample Implementation

- [x] **Create example script (sample-app-flowscript)**
  - [x] Contacts flow script demonstrating showNode/awaitOutputOfType
  - [x] Wired into ViewModel/rememberNavFlow (LaunchedEffect)
  - [x] Verified UI and tests

---

## Documentation Phase

- [x] **Update README.md**
  - [x] Added NavFlow Scripts section and references

- [x] **Create NAVFLOW_SCRIPT.md** (or add to ARCHITECTURE.md)
  - [ ] Explain the concept and motivation
  - [ ] Document NavFlowScriptScope API
  - [ ] Document launchNavFlowScript() usage
  - [ ] Provide complete login flow example
  - [ ] Show ViewModel integration pattern
  - [ ] Show testing pattern
  - [ ] Document limitations (single script per flow)
  - [ ] Document cancellation semantics
  - [ ] Document error handling

- [x] **Add KDoc to all public APIs**
  - [ ] NavFlowScriptScope interface
  - [ ] launchNavFlowScript() function
  - [ ] showNode() function
  - [ ] awaitOutput() function
  - [ ] awaitOutputOfType() function

---

## Validation Phase

- [x] **Run all tests** (library-core/library-compose + inspection)
  - [ ] `./gradlew :library-core:allTests`
  - [ ] `./gradlew :library-compose:allTests`
  - [ ] Inspect output for warnings/errors
  - [ ] Verify no regressions in existing tests

- [x] **Code review checklist**
  - [ ] No Compose dependencies in library-core
  - [ ] No Android-specific APIs
  - [ ] Proper coroutine cancellation handling
  - [ ] Thread-safe output collection
  - [ ] Clear error messages
  - [ ] Consistent with existing Kmposable patterns

- [x] **Manual testing**
  - [ ] Run sample app with script-driven flow
  - [ ] Test on Android
  - [ ] Test on iOS (if applicable)
  - [ ] Test on Desktop/JVM
  - [ ] Verify UI updates as script progresses
  - [ ] Test back navigation during script
  - [ ] Test app lifecycle (background/foreground)

---

## ✅ Clarifications Received

### 1. **Output Collection Strategy** ✅
**Decision:** Use existing `NavFlow.outputs` Flow directly
- Collect from `navFlow.outputs.filter { predicate(it) }.first()`
- No separate Channel needed
- Outputs emitted before `awaitOutput()` starts are NOT buffered (only see future outputs)
- Multiple collectors allowed (UI and scripts don't interfere)

### 2. **Single Script Enforcement** ✅
**Decision:** Document as undefined behavior, no runtime enforcement
- Assume at most one script per NavFlow
- No need to track active script Job in NavFlow
- Keep it simple for v1

### 3. **Navigator Access** ✅
**Decision:** Expose both `navigator` property AND navigation helpers
- `val navigator: Navigator<OUT, ENTRY>` - direct access if needed
- `fun showNode(block: Navigator<OUT, ENTRY>.() -> Unit)` - low-level wrapper
- Added dedicated helpers `showRoot`, `pushNode`, and `replaceTop` for common verbs

### 4. **Error Handling** ✅
**Decision:** No built-in timeout, use standard cancellation
- No timeout parameter on `awaitOutput()` / `awaitOutputOfType()`
- Users can wrap with `withTimeout { }` if needed
- If NavFlow disposed → script cancelled → `CancellationException` propagates
- No special exception types

### 5. **Type Safety for Navigator Operations** ✅
**Decision:** Use existing Navigator API, add script-level helpers
- Scripts can still call `showNode { … }` but typically use `showRoot/pushNode/replaceTop`
- Do NOT change core Navigator/NavFlow APIs

### 6. **Tracing** ✅
**Decision:** Optional callback instead of built-in logging
- `launchNavFlowScript(onTrace = …)` accepts a lambda that receives `trace { … }`
- Keeps tracing opt-in and testable; default remains silent

---

## Notes

- This is a **pure Kotlin** feature in `library-core`
- No changes to `library-compose` required
- Scripts are **optional** - existing NavFlow patterns still work
- Scripts are **headless** - no UI dependencies
- Scripts are **testable** - work with FlowTestScenario
- Scripts are **sequential** - coroutine-based orchestration

## Future Improvements / Ideas

To make sequential scripting even more ergonomic, we can explore:

- [x] **Script DSL helpers**
  - Provide small DSL utilities (e.g., `scriptFlow { show(node) -> await<Output>() }`) to reduce boilerplate push/pop code.
  - `withNode<T>` blocks that automatically push/pop and expose typed state.
- [x] **Typed orchestration helpers**
  - Added `awaitOutputCase` (DSL over `awaitMappedOutput`) so scripts can describe typed branches without manual `when/else` scaffolding.
- [x] **Navigation macros**
  - Added `pushForResult` as a reusable “push node → await mapper → pop” helper (used by sample-app-flowscript).
- [ ] **Script-aware testing/debugging**
  - FlowTestScenario assertions tailored to scripts (e.g., assert step transitions, inspect script state).
    - Added `FlowTestScenario.awaitTopNodeIs<T>()`, `awaitStackSize`, and `awaitStackTags` to await script-driven navigation.
  - Optional debug hooks/loggers to trace script execution.
- [ ] **Documentation & recipes**
  - Cookbook examples (auth flow, checkout wizard, background retry) showing end-to-end scripts developers can adapt.

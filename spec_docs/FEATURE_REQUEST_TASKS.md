# Feature Requests Tasklist (NavDiff, Effects, pushForResult)

## Quick take (opinions)
- **Navigation diff utilities**: High value, low risk. Pure helper; great for host animations and avoiding recomposition churn.
- **Effect channel support**: Useful pattern; prefer opt-in interface + base class so we don’t overload OUTPUT. Keep host consumption guidance in docs.
- **Result-based navigation (pushForResult)**: Good ergonomics but needs careful stack/removal handling to avoid leaks or double-pop. Make behavior explicit in KDoc/tests.

## Tasklist

- [ ] **Design/Confirm API details**
  - [ ] Validate package/file layout matches project conventions (`core/nav/NavDiff.kt`, `core/Effects.kt`, `core/ResultNode.kt`, `core/nav/KmposableNavigatorExtensions.kt`).
  - [ ] Confirm equality contract for nav diffs (reference equality for entries/nodes).
  - [ ] Confirm cancellation semantics for `pushForResult` (what happens if caller cancels).

- [ ] **Feature A: Navigation diff utilities**
  - [ ] Implement `NavDiff` data class with `isNoOp`.
  - [ ] Implement `diffNavState(prev, current)` using longest common prefix + reference equality.
  - [ ] Add unit tests covering push, pop, replaceAll, divergent stacks, first emission (prev = null).
  - [ ] Add KDoc and brief docs snippet (guides/reference).

- [ ] **Feature B: Effect channel support**
  - [ ] Add `EffectSource<EFFECT>`, `EffectHandler<EFFECT>`, and `EffectfulStatefulNode` with buffered `effects` flow plus `emitEffect/tryEmitEffect`.
  - [ ] Ensure no breaking changes to `StatefulNode`; base class is purely additive.
  - [ ] Add unit tests for effect emission/buffering/cancellation.
  - [ ] Document host consumption pattern (attach/detach collection) in guides/reference.

- [ ] **Feature C: Result-based navigation**
  - [ ] Add `KmposableResult` (Ok/Canceled) and `ResultNode<RESULT>`.
  - [ ] Implement `KmposableNavigator.pushForResult` (reified RESULT):
    - [ ] Type-check node implements `ResultNode<RESULT>`; throw `IllegalArgumentException` otherwise.
    - [ ] Push entry, select first of: node result vs entry removal → return Ok/Canceled.
    - [ ] On result, pop/remove entry if still present; avoid double-pop if already gone.
    - [ ] Respect structured concurrency; no leaked coroutines.
  - [ ] Unit tests: result wins, stack removal → Canceled, wrong node type → exception, caller cancellation behavior.
  - [ ] KDoc + docs snippet showing usage.

- [ ] **Docs & Samples**
  - [ ] Update guides/reference to introduce NavDiff, effects, and `pushForResult` with small examples.
  - [ ] Consider adding a minimal sample node demonstrating `EffectfulStatefulNode` + `ResultNode` (could be in docs or a small test node).

- [ ] **QA/Verification**
  - [ ] Run `./gradlew :library-core:allTests`, `:library-compose:allTests`, and `:library-compose:connectedAndroidDeviceTest`.
  - [ ] Inspect Gradle logs per AGENTS guidance.

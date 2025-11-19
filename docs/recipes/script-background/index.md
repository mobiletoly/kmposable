---
layout: doc
title: Script – Background Retry
permalink: /cookbook/recipes/script-background/
---

```kotlin
suspend fun NavFlowScriptScope<SyncOutput, *>.runSyncFlow(
    repository: SyncRepository,
    scope: CoroutineScope
) {
    showRoot { SyncStatusNode(scope) }

    val retryJob = navFlow.launchNavFlowScript(scope, onTrace = { msg -> trace { "[retry] $msg" } }) {
        repeat(5) { attempt ->
            trace { "Retry attempt $attempt" }
            val success = repository.syncOnce()
            if (success) {
                pushNode { SyncSuccessBannerNode(scope) }
                return@launchNavFlowScript
            }
            kotlinx.coroutines.delay(1_000)
        }
        pushNode { SyncGiveUpNode(scope) }
    }

    when (awaitOutputCase<SyncAction> {
        on<SyncOutput.UserCancelled> { SyncAction.Cancel }
        on<SyncOutput.SyncFinished> { SyncAction.Done }
    }) {
        SyncAction.Cancel -> retryJob.cancel()
        SyncAction.Done -> trace { "Sync completed" }
    }
}
```

Shows how a script can launch a child script (retry job) while keeping the main flow responsive.

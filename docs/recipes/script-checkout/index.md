---
layout: doc
title: Script – Checkout Flow
permalink: /cookbook/recipes/script-checkout/
---

```kotlin
fun NavFlow<OrderOutput, *>.launchCheckoutScript(
    scope: CoroutineScope,
    repositories: CheckoutRepositories,
    onTrace: (String) -> Unit = { }
) = launchNavFlowScript(scope, onTrace = onTrace) {
    showRoot { CartReviewNode(scope) }
    trace { "Checkout: showing cart" }
    awaitOutputOfType<OrderOutput.CartConfirmed>()

    showRoot { ShippingAddressNode(scope) }
    val address = awaitOutputOfType<OrderOutput.AddressEntered>()
    repositories.address.save(address.value)

    showRoot { PaymentNode(scope) }
    val paymentResult = awaitOutputCase<PaymentAction> {
        on<OrderOutput.PaymentAuthorized> { PaymentAction.Success(it.paymentId) }
        on<OrderOutput.PaymentFailed> { PaymentAction.Failure(it.reason) }
    }

    when (paymentResult) {
        is PaymentAction.Success -> {
            trace { "Checkout: payment authorized ${paymentResult.id}" }
            showRoot { ConfirmationNode(scope, paymentResult.id) }
        }
        is PaymentAction.Failure -> {
            trace { "Checkout: payment failure ${paymentResult.reason}" }
            pushNode { PaymentErrorNode(scope, paymentResult.reason) }
            awaitOutputOfType<OrderOutput.PaymentRetry>()
            navFlow.pop()
            replaceTop { PaymentNode(scope) }
        }
    }
}
```

When to use:
- Multi-step checkout/wizard flows where policy, retries, and tracing belong in one place.

Why it matters:
- Centralises checkout policy (cart → shipping → payment → confirmation/retry) in one coroutine.
- Shows tracing, branching, and reuse of helpers (`awaitOutputCase`, `trace`, `pushNode`).
- Nodes stay focused on UI/state; payment retry/branch logic lives here and is easy to test headlessly.

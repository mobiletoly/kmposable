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

Illustrates branching, tracing, and reusing helpers (`awaitOutputCase`, `trace`, `pushNode`).

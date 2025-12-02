package dev.goquick.kmposable.core

/**
 * Marker for overlay nodes that should pop themselves when a result is produced.
 *
 * Overlay hosts may observe [result] and pop the entry when [shouldAutoClose] returns true
 * (defaults to closing on any [KmposableResult.Ok] or [KmposableResult.Canceled]).
 */
interface AutoCloseOverlay<RESULT : Any> : ResultNode<RESULT> {
    fun shouldAutoClose(result: KmposableResult<RESULT>): Boolean = when (result) {
        is KmposableResult.Ok -> true
        KmposableResult.Canceled -> true
    }
}

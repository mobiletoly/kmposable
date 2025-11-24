package dev.goquick.kmposable.core

import kotlinx.coroutines.flow.Flow

/** Result emitted by a result-producing node. */
sealed interface KmposableResult<out R> {
    data class Ok<R>(val value: R) : KmposableResult<R>
    data object Canceled : KmposableResult<Nothing>
}

/** Contract for nodes that can produce a single typed result independent of their outputs. */
interface ResultNode<RESULT : Any> {
    val result: Flow<KmposableResult<RESULT>>
}

package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.runtime.NavFlow

@Composable
expect fun <OUT : Any> KmposableBackHandler(runtime: NavFlow<OUT, *>)

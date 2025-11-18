/*
 * Copyright 2025 Toly Pochkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.goquick.kmposable.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fundamental unit of the headless runtime. A [Node] exposes immutable state that a renderer can
 * observe, accepts UI-driven events, and emits outputs for the parent/runtime to react to
 * (typically navigation).
 *
 * Node instances are treated as unique; every push onto the stack should create a fresh instance
 * rather than reusing an old one. The runtime keys lifecycle hooks and output collectors off the
 * node reference, so reusing instances can lead to missed attach/detach calls.
 */
interface Node<STATE : Any, EVENT : Any, OUTPUT : Any> {
    val state: StateFlow<STATE>
    fun onEvent(event: EVENT)
    val outputs: Flow<OUTPUT>
}

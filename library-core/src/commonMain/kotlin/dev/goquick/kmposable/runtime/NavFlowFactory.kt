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
package dev.goquick.kmposable.runtime

import dev.goquick.kmposable.core.nav.DefaultStackEntry
import dev.goquick.kmposable.core.nav.KmposableStackEntry

/**
 * Factory abstraction that produces fresh [NavFlow] instances for a given node container.
 * UI layers and tests can depend on the same implementation to ensure parity between headless and
 * rendered environments.
 */
fun interface NavFlowFactory<OUT : Any, ENTRY : KmposableStackEntry<OUT>> {
    fun createNavFlow(): NavFlow<OUT, ENTRY>
}

/** Convenience alias for NavFlow that rely on the default stack entry implementation. */
typealias SimpleNavFlowFactory<OUT> = NavFlowFactory<OUT, DefaultStackEntry<OUT>>

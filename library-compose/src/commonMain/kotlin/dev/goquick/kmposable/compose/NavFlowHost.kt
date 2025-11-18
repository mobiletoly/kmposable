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
package dev.goquick.kmposable.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goquick.kmposable.runtime.NavFlow

/** Renders the top kmposable node and optionally wires the shared back handler. */
@Composable
fun <OUT : Any> NavFlowHost(
    navFlow: NavFlow<OUT, *>,
    renderer: NodeRenderer<OUT>,
    enableBackHandler: Boolean = true,
) {
    if (enableBackHandler) {
        KmposableBackHandler(navFlow)
    }
    val navState by navFlow.navState.collectAsState()
    renderer.Render(navState.top)
}

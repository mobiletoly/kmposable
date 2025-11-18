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

/**
 * Optional contract for nodes that want to know when they enter or leave the navigation tree.
 *
 * A node is *attached* when it becomes part of the active stack – either because it is pushed,
 * becomes the new root via `replaceAll`, or the runtime starts for the first time. A node is
 * *detached* every time it leaves the stack: pop, replace, popAll, popTo, or runtime disposal.
 * Attach/detach can therefore be invoked multiple times during a node's lifetime if the same
 * instance is reintroduced.
 */
interface LifecycleAwareNode {
    /** Called whenever the node transitions from not-present to present on the stack. */
    fun onAttach() {}

    /** Called right before the node is removed from the stack (pop/replace/dispose). */
    fun onDetach() {}
}

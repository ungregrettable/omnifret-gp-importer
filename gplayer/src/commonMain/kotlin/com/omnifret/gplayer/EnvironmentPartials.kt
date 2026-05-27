/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer

import com.omnifret.gplayer.collections.DoubleList
import com.omnifret.gplayer.platform.Json
import kotlin.contracts.ExperimentalContracts

// KMP-shaped replacement for upstream alphaTab's Android EnvironmentPartials.
// Worker scopes (originally used to drive alphaTab's parallel rendering
// inside a JS Web Worker / Android background thread) are not supported
// in the KMP port — the host app's coroutine scope is the integration
// point. The other partial methods are simple delegates.
@OptIn(ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
internal class EnvironmentPartials {
    companion object {
        internal fun _createPlatformSpecificRenderEngines(
            @Suppress("UNUSED_PARAMETER") engines: com.omnifret.gplayer.collections.Map<String, RenderEngineFactory>
        ) {
            // Intentionally empty. The host registers ICanvas
            // implementations via the platform-specific entry points;
            // alphaTab's built-in "skia" engine is replaced by Android
            // Compose / iOS CoreGraphics canvases.
        }

        internal fun _printPlatformInfo(print: (message: String) -> Unit) {
            print("Platform: ${PlatformInfo.name}")
            print("Version: ${PlatformInfo.version}")
        }

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun quoteJsonString(string: String) = Json.quoteJsonString(string)

        @Suppress("NOTHING_TO_INLINE")
        internal inline fun sortDescending(list: DoubleList) = list.sortDescending()

        internal inline fun <reified T> getGlobalWorkerScope(): Nothing {
            throw UnsupportedOperationException(
                "Worker scopes are not supported in the KMP port; run the " +
                "renderer/synth on the host's coroutine dispatcher."
            )
        }

        inline fun <reified T> prepareForPostMessage(v: T) = v
    }
}

// Per-platform device info; populated by androidMain / iosMain.
internal expect object PlatformInfo {
    val name: String
    val version: String
}

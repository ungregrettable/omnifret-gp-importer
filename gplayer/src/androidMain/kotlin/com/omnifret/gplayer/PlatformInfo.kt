/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer

import android.os.Build

internal actual object PlatformInfo {
    actual val name: String = "Android (${Build.MANUFACTURER} ${Build.MODEL})"
    actual val version: String = "API ${Build.VERSION.SDK_INT}"
}

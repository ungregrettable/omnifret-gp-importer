/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer.generated

// Upstream alphaTab generates this file at build time, baking in the
// commit hash and semver. We snapshot at a fixed alphaTab version, so the
// values are constants. If we ever bump the alphaTab snapshot, also bump
// these to match.
internal object VersionInfo {
    val version: String = "1.9.0-snapshot"
    val commit: String = "kmp-port"

    fun print(printer: (message: String) -> Unit) {
        printer("GPlayer v$version (alphaTab snapshot $commit)")
    }
}

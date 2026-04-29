/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer.api

import com.omnifret.gplayer.Settings
import com.omnifret.gplayer.core.ecmaScript.Uint8Array
import com.omnifret.gplayer.importer.ScoreLoader
import com.omnifret.gplayer.importer.UnsupportedFormatError
import com.omnifret.gplayer.model.Score

// Public entry point for OmniFret. The smallest stable surface that
// exposes parsing + playback. Rendering is published separately under
// [ScoreSvgRenderer] so a caller that only needs to ingest tabs doesn't
// depend on the SVG / Canvas stack.
//
// Parsing and playback logic is derived from alphaTab
// (https://github.com/CoderLine/alphaTab, MPL-2.0).

/**
 * Parse a Guitar Pro / MusicXML / Capella / GPlayerTex file. Auto-detects
 * format by trying every importer in turn; throws
 * [UnsupportedFormatException] if none match.
 *
 * Pure Kotlin — runs on commonMain, no Android/iOS APIs.
 */
@OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
public object GPlayer {

    /** Parse from raw bytes (any supported binary or text format). */
    public fun parseScore(bytes: ByteArray, settings: Settings? = null): Score {
        return try {
            ScoreLoader.loadScoreFromBytes(Uint8Array(bytes.toUByteArray()), settings)
        } catch (e: UnsupportedFormatError) {
            throw UnsupportedFormatException(e.message ?: "no compatible importer found")
        }
    }

    /** Parse GPlayerTex source. */
    public fun parseGPlayerTex(tex: String, settings: Settings? = null): Score {
        return ScoreLoader.loadGPlayerTex(tex, settings)
    }

    /** Build a default [Settings] instance. Convenience for Java callers. */
    public fun defaultSettings(): Settings = Settings()
}

/**
 * Thrown when no importer can parse the supplied bytes. Callers should
 * surface a user-friendly "unsupported file format" message.
 */
public class UnsupportedFormatException(message: String) : Exception(message)

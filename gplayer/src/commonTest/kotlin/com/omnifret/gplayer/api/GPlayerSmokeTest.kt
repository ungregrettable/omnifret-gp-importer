/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer.api

import kotlin.contracts.ExperimentalContracts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Smoke tests that exercise the full pipeline end-to-end on commonMain,
 * so they run on every target (Android JVM + iOS native simulator).
 * They use GPlayerTex source (a plain string format) so we don't need a
 * binary GP fixture.
 */
@OptIn(ExperimentalContracts::class, ExperimentalUnsignedTypes::class)
class GPlayerSmokeTest {

    private val sampleTex = """
        \title "Test Score"
        \tempo 120
        .
        :4 3.3 5.3 7.3 8.3 |
        :4 3.3 5.3 7.3 8.3
    """.trimIndent()

    @Test
    fun parses_alphatex_source() {
        val score = GPlayer.parseGPlayerTex(sampleTex)
        assertEquals("Test Score", score.title)
        assertTrue(score.tracks.length > 0.0, "expected at least one track")
    }

    @Test
    fun parses_via_byte_entry_point() {
        // The bytes path tries every importer; GPlayerTex falls through
        // because its detection is text-prefix based.
        val score = GPlayer.parseScore(sampleTex.encodeToByteArray())
        assertNotNull(score)
        assertEquals("Test Score", score.title)
    }

    @Test
    fun playback_builder_emits_note_events() {
        val score = GPlayer.parseGPlayerTex(sampleTex)
        val notes = mutableListOf<Triple<Int, Int, Double>>() // key, velocity, startTick
        var tempoSeen = 0.0

        val listener = object : PlaybackEventListener {
            override fun onTimeSignature(tick: Double, numerator: Int, denominator: Int) {}
            override fun onTempo(tick: Double, bpm: Double) { tempoSeen = bpm }
            override fun onNote(
                track: Int,
                channel: Int,
                startTick: Double,
                lengthTicks: Double,
                midiKey: Int,
                velocity: Int,
            ) {
                notes += Triple(midiKey, velocity, startTick)
            }
        }

        PlaybackBuilder(score).generate(listener)

        assertTrue(notes.isNotEmpty(), "expected note events from playback generation")
        assertEquals(120.0, tempoSeen, "tempo should match \\tempo 120 directive")
    }
}

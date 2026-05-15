/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

@file:OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)

package com.omnifret.gplayer.api

import com.omnifret.gplayer.test.parseFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SvgLazyRenderTest {

    @Test
    fun lazy_chunk_count_matches_eager() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val eager = ScoreSvgRenderer(score).render()
        val lazy = ScoreSvgRenderer(score).openLazy()
        try {
            assertEquals(eager.chunks.size, lazy.chunks.size, "chunk count differs")
        } finally {
            lazy.close()
        }
    }

    @Test
    fun lazy_chunk_layout_matches_eager() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val eager = ScoreSvgRenderer(score).render()
        val lazy = ScoreSvgRenderer(score).openLazy()
        try {
            assertEquals(eager.totalWidthPx, lazy.totalWidthPx)
            assertEquals(eager.totalHeightPx, lazy.totalHeightPx)
            for (i in eager.chunks.indices) {
                assertEquals(eager.chunks[i].firstBarIndex, lazy.chunks[i].firstBarIndex)
                assertEquals(eager.chunks[i].lastBarIndex, lazy.chunks[i].lastBarIndex)
                assertEquals(eager.chunks[i].widthPx, lazy.chunks[i].widthPx)
                assertEquals(eager.chunks[i].heightPx, lazy.chunks[i].heightPx)
            }
        } finally {
            lazy.close()
        }
    }

    @Test
    fun renderChunk_returns_valid_svg_chunk() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val lazy = ScoreSvgRenderer(score).openLazy()
        try {
            val rendered = lazy.renderChunk(0)
            assertTrue(rendered.svg.trimStart().startsWith("<svg"))
            assertEquals(lazy.chunks[0].firstBarIndex, rendered.firstBarIndex)
            assertEquals(lazy.chunks[0].lastBarIndex, rendered.lastBarIndex)
            assertEquals(lazy.chunks[0].widthPx, rendered.widthPx)
            assertEquals(lazy.chunks[0].heightPx, rendered.heightPx)
        } finally {
            lazy.close()
        }
    }

    @Test
    fun renderChunk_is_idempotent() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val lazy = ScoreSvgRenderer(score).openLazy()
        try {
            val first = lazy.renderChunk(0)
            val second = lazy.renderChunk(0)
            assertEquals(first.svg, second.svg)
        } finally {
            lazy.close()
        }
    }

    @Test
    fun close_then_renderChunk_throws() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val lazy = ScoreSvgRenderer(score).openLazy()
        lazy.close()
        assertFails { lazy.renderChunk(0) }
    }

    @Test
    fun close_is_idempotent() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val lazy = ScoreSvgRenderer(score).openLazy()
        lazy.close()
        lazy.close()
    }

    @Test
    fun renderChunk_out_of_range_throws() {
        val score = parseFixture("guitarpro5/notes.gp5")
        val lazy = ScoreSvgRenderer(score).openLazy()
        try {
            assertFails { lazy.renderChunk(-1) }
            assertFails { lazy.renderChunk(lazy.chunks.size) }
        } finally {
            lazy.close()
        }
    }
}

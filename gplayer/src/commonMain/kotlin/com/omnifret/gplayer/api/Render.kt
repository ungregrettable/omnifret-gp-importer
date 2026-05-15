/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 omnifret-gplayer contributors.
 */

package com.omnifret.gplayer.api

import com.omnifret.gplayer.Settings
import com.omnifret.gplayer.collections.DoubleList
import com.omnifret.gplayer.model.Score
import com.omnifret.gplayer.model.Track
import com.omnifret.gplayer.rendering.RenderFinishedEventArgs
import com.omnifret.gplayer.rendering.ScoreRenderer

// Rendering wrapper for OmniFret. We use alphaTab's existing SVG output
// path (CssFontSvgCanvas) because:
//   - It's already in commonMain, no per-platform impl needed.
//   - The output is a string of SVG markup that any UI layer can render
//     (Compose: AndroidSvg or rasterise; SwiftUI: SVGKit / WKWebView).
//   - It bypasses the entire glyph/font measurement subsystem at the
//     drawing level — fonts are rendered as text with @font-face CSS.
//
// Custom platform Canvas implementations (Compose Canvas on Android,
// CoreGraphics on iOS) would be a separate Phase 3 follow-up; the
// `com.omnifret.gplayer.platform.ICanvas` interface is the registered seam if the
// OmniFret team wants to skip SVG and draw natively.

/**
 * One rendered chunk of a score. Multiple chunks are emitted for long
 * scores (page-by-page or system-by-system depending on
 * [LayoutMode][com.omnifret.gplayer.LayoutMode]). Stitch them together vertically
 * for the final display.
 */
public data class ScoreRenderChunk(
    val svg: String,
    val widthPx: Double,
    val heightPx: Double,
    val firstBarIndex: Int,
    val lastBarIndex: Int,
)

/** Result of a full-score render. */
public data class ScoreRenderResult(
    val totalWidthPx: Double,
    val totalHeightPx: Double,
    val chunks: List<ScoreRenderChunk>,
)

/** Layout-only metadata for one lazily rendered SVG chunk. */
public data class ScoreSvgChunkLayout(
    val index: Int,
    val firstBarIndex: Int,
    val lastBarIndex: Int,
    val widthPx: Double,
    val heightPx: Double,
    internal val resultId: String,
)

/**
 * Render a [Score] to SVG. Synchronous — for large scores call from a
 * background dispatcher. Use [trackIndexes] to render a subset of the
 * score's tracks; null renders all tracks.
 *
 * Width should match the display surface width in CSS pixels (1px =
 * 1 layout unit). The renderer reflows to fit; pass the actual viewport
 * width for best results.
 */
@OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
public class ScoreSvgRenderer(
    private val score: Score,
    private val tracks: List<Track>? = null,
    private val widthPx: Double = 970.0,
    settings: Settings? = null,
) {
    private val resolvedSettings: Settings = (settings ?: Settings()).also {
        // Force the "svg" engine — alphaTab also exposes "skia", but
        // KMP-port stripped it (see Environment.kt).
        it.core.engine = "svg"
    }

    public fun render(): ScoreRenderResult {
        // The layout's default lazy-loading mode emits only
        // `partialLayoutFinished` and stores partials for `renderLazyPartial`.
        // We expose a synchronous `render()` that returns chunks directly,
        // so disable lazy loading to force the eager render path that
        // emits `partialRenderFinished` with `renderResult` populated.
        resolvedSettings.core.enableLazyLoading = false
        val renderer = ScoreRenderer(resolvedSettings)
        renderer.width = widthPx

        val chunks = mutableListOf<ScoreRenderChunk>()
        var totalW = 0.0
        var totalH = 0.0
        var caughtError: Throwable? = null

        renderer.partialRenderFinished.on { args: RenderFinishedEventArgs ->
            val svg = args.renderResult as? String ?: return@on
            chunks += ScoreRenderChunk(
                svg = svg,
                widthPx = args.width,
                heightPx = args.height,
                firstBarIndex = args.firstMasterBarIndex.toInt(),
                lastBarIndex = args.lastMasterBarIndex.toInt(),
            )
        }
        renderer.renderFinished.on { args: RenderFinishedEventArgs ->
            totalW = args.totalWidth
            totalH = args.totalHeight
        }
        renderer.error.on { e -> caughtError = e }

        // Convert track list to the DoubleList that alphaTab expects.
        // Important: ScoreRenderer.renderScore short-circuits to "render
        // nothing" if `trackIndexes == null`. To get the documented
        // "null means all tracks" behavior, pass an empty DoubleList
        // instead — the renderer's empty-list branch slices all tracks.
        val indexes: DoubleList = if (tracks == null) {
            DoubleList()
        } else {
            val all = score.tracks
            val list = DoubleList()
            for (t in tracks) {
                for (i in 0 until all.length.toInt()) {
                    if (all[i] === t) {
                        list.push(i.toDouble())
                        break
                    }
                }
            }
            list
        }
        try {
            renderer.renderScore(score, indexes, null)
            caughtError?.let { throw it }

            return ScoreRenderResult(
                totalWidthPx = totalW,
                totalHeightPx = totalH,
                chunks = chunks.toList(),
            )
        } finally {
            renderer.destroy()
        }
    }

    /**
     * Open a lazy SVG render handle. This runs layout immediately and
     * returns chunk metadata; call [ScoreSvgLazyHandle.renderChunk] to
     * materialise SVG markup for a specific chunk.
     */
    public fun openLazy(): ScoreSvgLazyHandle {
        resolvedSettings.core.enableLazyLoading = true
        val renderer = ScoreRenderer(resolvedSettings)
        renderer.width = widthPx

        val layouts = mutableListOf<ScoreSvgChunkLayout>()
        var totalW = 0.0
        var totalH = 0.0
        var caughtError: Throwable? = null

        renderer.partialLayoutFinished.on { args: RenderFinishedEventArgs ->
            layouts += ScoreSvgChunkLayout(
                index = layouts.size,
                firstBarIndex = args.firstMasterBarIndex.toInt(),
                lastBarIndex = args.lastMasterBarIndex.toInt(),
                widthPx = args.width,
                heightPx = args.height,
                resultId = args.id,
            )
        }
        renderer.renderFinished.on { args: RenderFinishedEventArgs ->
            totalW = args.totalWidth
            totalH = args.totalHeight
        }
        renderer.error.on { e -> caughtError = e }

        try {
            renderer.renderScore(score, resolvedTrackIndexes(), null)
            caughtError?.let { throw it }

            return ScoreSvgLazyHandle(
                renderer = renderer,
                totalWidthPx = totalW,
                totalHeightPx = totalH,
                chunks = layouts.toList(),
            )
        } catch (t: Throwable) {
            renderer.destroy()
            throw t
        }
    }

    private fun resolvedTrackIndexes(): DoubleList {
        if (tracks == null) return DoubleList()
        val all = score.tracks
        val list = DoubleList()
        for (t in tracks) {
            for (i in 0 until all.length.toInt()) {
                if (all[i] === t) {
                    list.push(i.toDouble())
                    break
                }
            }
        }
        return list
    }
}

/** On-demand SVG handle returned by [ScoreSvgRenderer.openLazy]. */
@OptIn(kotlin.contracts.ExperimentalContracts::class, kotlin.ExperimentalUnsignedTypes::class)
public class ScoreSvgLazyHandle internal constructor(
    private var renderer: ScoreRenderer?,
    public val totalWidthPx: Double,
    public val totalHeightPx: Double,
    public val chunks: List<ScoreSvgChunkLayout>,
) {

    public fun renderChunk(index: Int): ScoreRenderChunk {
        val activeRenderer = renderer
        check(activeRenderer != null) { "ScoreSvgLazyHandle has been closed" }
        require(index in chunks.indices) {
            "chunk index $index out of range ${chunks.indices}"
        }

        val layout = chunks[index]
        var captured: ScoreRenderChunk? = null
        var caughtError: Throwable? = null
        val listener: (RenderFinishedEventArgs) -> Unit = { args ->
            if (args.id == layout.resultId) {
                val svg = args.renderResult as? String
                if (svg != null) {
                    captured = ScoreRenderChunk(
                        svg = svg,
                        widthPx = layout.widthPx,
                        heightPx = layout.heightPx,
                        firstBarIndex = layout.firstBarIndex,
                        lastBarIndex = layout.lastBarIndex,
                    )
                }
            }
        }
        val errorListener: (Throwable) -> Unit = { caughtError = it }
        activeRenderer.partialRenderFinished.on(listener)
        activeRenderer.error.on(errorListener)
        try {
            activeRenderer.renderResult(layout.resultId)
            caughtError?.let { throw it }
            return captured
                ?: error("No partialRenderFinished event for resultId=${layout.resultId}")
        } finally {
            activeRenderer.partialRenderFinished.off(listener)
            activeRenderer.error.off(errorListener)
        }
    }

    public fun close() {
        val activeRenderer = renderer ?: return
        renderer = null
        activeRenderer.destroy()
    }
}

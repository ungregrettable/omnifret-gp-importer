package com.omnifret.gplayer.platform.svg

import com.omnifret.gplayer.Logger
import com.omnifret.gplayer.collections.DoubleList
import com.omnifret.gplayer.core.ecmaScript.Uint8Array
import kotlin.contracts.ExperimentalContracts

// KMP-PORT: the original Android impl uses GPlayerSkia to measure glyph
// widths so the SVG renderer can produce pixel-accurate output. GPlayerSkia
// is JVM/JNI-only, so on the KMP port we delegate to a platform-supplied
// MusicFontMeasurer (expect/actual) that wraps Compose's TextMeasurer on
// Android and Core Text on iOS. Each platform fills the lookup table on
// first use; if the platform implementation is unavailable we fall back
// to a uniform 8x10 metric so the renderer still produces valid SVG.
internal class FontSizesPartials {
    companion object {
        @ExperimentalUnsignedTypes
        @ExperimentalContracts
        public fun generateFontLookup(family: String) {
            if (FontSizes.fontSizeLookupTables.has(family)) {
                return
            }

            try {
                val measurer = MusicFontMeasurer.instance
                if (measurer != null) {
                    val widths = DoubleList()
                    val heights = DoubleList()
                    val measureSize = 11.0
                    for (i in (FontSizes.ControlChars.toInt() until 255)) {
                        val ch = i.toChar().toString()
                        val (w, h) = measurer.measure(family, ch, measureSize)
                        widths.push(w)
                        heights.push(h)
                    }
                    FontSizes.fontSizeLookupTables.set(
                        family,
                        FontSizeDefinition(Uint8Array(widths), Uint8Array(heights))
                    )
                    return
                }

                Logger.warning(
                    "Rendering",
                    "Generating font lookup with no MusicFontMeasurer registered; " +
                        "SVG sizes will use a fallback uniform metric."
                )
            } catch (e: Throwable) {
                Logger.error("Rendering", "Error while generating font lookup $e")
            }
            FontSizes.fontSizeLookupTables.set(
                family,
                FontSizeDefinition(
                    Uint8Array(ubyteArrayOf((8).toUByte())),
                    Uint8Array(ubyteArrayOf((10).toUByte()))
                )
            )
        }
    }
}

// Platform glue. Wires through to Compose TextMeasurer (Android) or Core
// Text (iOS) — see Phase 3. The host app must call
// MusicFontMeasurer.register(...) once at startup before triggering a
// render. If unset, FontSizesPartials uses a uniform fallback metric.
internal interface MusicFontMeasurer {
    /** Returns (widthPx, heightPx) for [text] at [sizePx] in [family]. */
    fun measure(family: String, text: String, sizePx: Double): Pair<Double, Double>

    companion object {
        var instance: MusicFontMeasurer? = null
            private set

        fun register(measurer: MusicFontMeasurer) {
            instance = measurer
        }
    }
}

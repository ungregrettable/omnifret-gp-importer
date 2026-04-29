package com.omnifret.gplayer.core.ecmaScript

import kotlin.time.TimeSource

internal class Date {
    companion object {
        // Monotonic, suitable for "elapsed time" telemetry. The transpiled
        // the library uses Date.now() exclusively for diagnostics
        // (logger timestamps) — wall-clock parity is unnecessary, so we
        // avoid the kotlinx-datetime dependency.
        private val origin = TimeSource.Monotonic.markNow()

        public fun now(): Double {
            return origin.elapsedNow().inWholeMicroseconds.toDouble() / 1000.0
        }
    }
}

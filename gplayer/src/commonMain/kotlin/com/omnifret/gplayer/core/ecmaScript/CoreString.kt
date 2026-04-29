package com.omnifret.gplayer.core.ecmaScript

internal class CoreString {
    companion object {
        public fun fromCharCode(code: Double): String {
            return code.toInt().toChar().toString()
        }

        // Code-point-aware string construction: a single Unicode code
        // point can require a surrogate pair (UTF-16) for values above
        // U+FFFF. Kotlin/Native lacks the JVM's String(IntArray) ctor, so
        // do the surrogate-pair encoding by hand to stay platform-portable.
        public fun fromCodePoint(code: Double): String {
            return appendCodePoint(StringBuilder(), code.toInt()).toString()
        }

        public fun fromCodePoint(vararg code: Double): String {
            val sb = StringBuilder()
            for (c in code) appendCodePoint(sb, c.toInt())
            return sb.toString()
        }

        private fun appendCodePoint(sb: StringBuilder, codePoint: Int): StringBuilder {
            if (codePoint < 0x10000) {
                sb.append(codePoint.toChar())
            } else {
                val v = codePoint - 0x10000
                val high = 0xD800 or (v ushr 10)
                val low = 0xDC00 or (v and 0x3FF)
                sb.append(high.toChar())
                sb.append(low.toChar())
            }
            return sb
        }
    }
}

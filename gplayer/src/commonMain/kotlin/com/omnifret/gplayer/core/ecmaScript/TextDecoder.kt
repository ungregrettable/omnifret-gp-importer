package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.core.decodeToString
import com.omnifret.gplayer.core.ecmaScript.ArrayBuffer

internal class TextDecoder(encoding:String) {
    private val _encoding:String = encoding

    @ExperimentalUnsignedTypes
    public fun decode(buffer: ArrayBuffer): String {
        return buffer.decodeToString(_encoding)
    }
}

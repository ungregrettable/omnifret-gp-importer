package com.omnifret.gplayer.core.ecmaScript

internal class TextEncoder {
    @ExperimentalUnsignedTypes
    public fun encode(str: String): com.omnifret.gplayer.core.ecmaScript.Uint8Array {
        return com.omnifret.gplayer.core.ecmaScript.Uint8Array(str.encodeToByteArray().toUByteArray())
    }
}

package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.platform.Json
import kotlin.contracts.ExperimentalContracts

internal class JSON {
    companion object {
        @ExperimentalContracts
        @ExperimentalUnsignedTypes
        fun stringify(v: String) = Json.quoteJsonString(v)
    }
}

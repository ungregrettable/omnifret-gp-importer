package com.omnifret.gplayer.importer.gplayertex

internal class SimpleGPlayerTexParameterDefinition {
    val length: Double
    val v0: com.omnifret.gplayer.collections.List<GPlayerTexNodeType>
    val v1: ArgumentListParseTypesMode
    val v2: com.omnifret.gplayer.collections.List<String>?
    val v3: com.omnifret.gplayer.collections.List<String>?

    public constructor(
        types: com.omnifret.gplayer.collections.List<GPlayerTexNodeType>,
        parseMode: ArgumentListParseTypesMode
    ) {
        v0 = types
        v1 = parseMode
        v2 = null
        v3 = null
        length = 2.0
    }

    public constructor(
        types: com.omnifret.gplayer.collections.List<GPlayerTexNodeType>,
        parseMode: ArgumentListParseTypesMode,
        allowedValues: com.omnifret.gplayer.collections.List<String>?
    ) {
        v0 = types
        v1 = parseMode
        v2 = allowedValues
        v3 = null
        length = 3.0
    }

    public constructor(
        types: com.omnifret.gplayer.collections.List<GPlayerTexNodeType>,
        parseMode: ArgumentListParseTypesMode,
        allowedValues: com.omnifret.gplayer.collections.List<String>?,
        reservedIdentifiers: com.omnifret.gplayer.collections.List<String>?
    ) {
        v0 = types
        v1 = parseMode
        v2 = allowedValues
        v3 = reservedIdentifiers
        length = 4.0
    }
}

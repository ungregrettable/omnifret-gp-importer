package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.core.ArrayTuple

public class Record<TKey, TValue> : com.omnifret.gplayer.collections.Map<TKey, TValue> {
    constructor() : super()
    constructor(vararg elements: ArrayTuple<TKey, TValue>) : super(elements.asIterable())
}

package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.collections.ArrayListWithRemoveRange
import com.omnifret.gplayer.collections.BooleanList
import com.omnifret.gplayer.collections.DoubleList
import com.omnifret.gplayer.collections.IDoubleIterable

@Suppress("NOTHING_TO_INLINE")
internal class Array {
    companion object {
        public inline fun <T> from(x: Iterable<T>): com.omnifret.gplayer.collections.List<T> {
            return com.omnifret.gplayer.collections.List(x.toCollection(ArrayListWithRemoveRange()))
        }
        public inline fun from(x: IDoubleIterable): com.omnifret.gplayer.collections.DoubleList {
            return com.omnifret.gplayer.collections.DoubleList(x)
        }
        public inline fun isArray(x:Any?):Boolean {
            return x is com.omnifret.gplayer.collections.List<*> || x is DoubleList || x is BooleanList
        }
    }
}

package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.core.decodeToFloatArray

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
public class Float32Array : Iterable<Float> {
    public val data: FloatArray

    internal val buffer: ArrayBuffer
        get() {
            // Manual little-endian IEEE-754 float32 encode. The buffer
            // result is a UByteArray, which our ArrayBuffer typealias
            // resolves to. Done by hand so commonMain is java.nio-free.
            val out = ByteArray(data.size * 4)
            for (i in data.indices) {
                val bits = data[i].toRawBits()
                out[i * 4] = (bits and 0xFF).toByte()
                out[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
                out[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
                out[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
            }
            return out.asUByteArray()
        }

    internal inline val byteOffset: Double
        get() = 0.0
    internal inline val byteLength: Double
        get() = (data.size * Float.SIZE_BYTES).toDouble()

    public inline val length: Double
        get() {
            return data.size.toDouble()
        }

    public constructor(size: Double) {
        data = FloatArray(size.toInt())
    }

    public constructor(x: FloatArray) {
        data = x
    }

    internal constructor(x: ArrayBuffer) {
        data = x.decodeToFloatArray()
    }

    internal constructor(x: Iterable<Double>) {
        this.data = x.map { d -> d.toFloat() }.toFloatArray()
    }

    public inline operator fun get(idx: Int): Double {
        return data[idx].toDouble()
    }

    public inline operator fun set(idx: Int, value: Double) {
        data[idx] = value.toFloat()
    }

    override fun iterator(): FloatIterator {
        return data.iterator()
    }

    internal inline fun set(subarray: Float32Array, pos: Double) {
        subarray.data.copyInto(data, pos.toInt(), 0, subarray.data.size)
    }

    internal fun subarray(begin: Double, end: Double): Float32Array {
        return Float32Array(data.copyOfRange(begin.toInt(), end.toInt()))
    }

    internal fun fill(value: Double, start: Double, end: Double) {
        data.fill(value.toFloat(), start.toInt(), end.toInt())
    }
}

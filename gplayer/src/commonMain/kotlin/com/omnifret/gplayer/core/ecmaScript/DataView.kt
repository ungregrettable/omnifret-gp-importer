package com.omnifret.gplayer.core.ecmaScript

// JS-style DataView, replacing the JVM ByteBuffer impl with manual byte
// reads so commonMain doesn't depend on java.nio. The transpiled code
// uses only getFloat32 (32-bit IEEE-754) and getInt16 (16-bit signed),
// both with explicit endian; if the transpiler ever emits more, add
// here.

@ExperimentalUnsignedTypes
internal class DataView {
    private val bytes: ByteArray
    private val baseOffset: Int

    val buffer: ArrayBuffer
    val byteOffset: Double
    val byteLength: Double

    constructor(buffer: ArrayBuffer) {
        this.buffer = buffer
        byteOffset = 0.0
        byteLength = buffer.size.toDouble()
        this.bytes = buffer.asByteArray()
        this.baseOffset = 0
    }

    constructor(buffer: ArrayBuffer, byteOffset: Double, byteLength: Double) {
        this.buffer = buffer
        this.byteOffset = byteOffset
        this.byteLength = byteLength
        this.bytes = buffer.asByteArray()
        this.baseOffset = byteOffset.toInt()
    }

    fun getFloat32(index: Double, littleEndian: Boolean): Double {
        val i = baseOffset + index.toInt()
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = bytes[i + 1].toInt() and 0xFF
        val b2 = bytes[i + 2].toInt() and 0xFF
        val b3 = bytes[i + 3].toInt() and 0xFF
        val bits = if (littleEndian) {
            (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        } else {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
        return Float.fromBits(bits).toDouble()
    }

    fun getInt16(index: Double, littleEndian: Boolean): Double {
        val i = baseOffset + index.toInt()
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = bytes[i + 1].toInt() and 0xFF
        val raw = if (littleEndian) (b1 shl 8) or b0 else (b0 shl 8) or b1
        // Sign-extend from 16 bits.
        val signed = if (raw and 0x8000 != 0) raw or 0xFFFF.inv() else raw
        return signed.toDouble()
    }
}

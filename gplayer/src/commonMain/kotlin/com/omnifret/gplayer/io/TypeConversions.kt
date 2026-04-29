package com.omnifret.gplayer.io

import com.omnifret.gplayer.core.ecmaScript.Uint8Array

@ExperimentalUnsignedTypes
internal class TypeConversions {
    companion object {
        public fun uint16ToInt16(v: Double): Double {
            return v.toUInt().toShort().toDouble()
        }

        public fun int16ToUint32(v: Double): Double {
            return v.toInt().toShort().toUInt().toDouble()
        }

        public fun int32ToUint16(v: Double): Double {
            return v.toInt().toUShort().toDouble()
        }

        public fun int32ToInt16(v: Double): Double {
            return v.toInt().toShort().toDouble()
        }

        public fun int32ToUint32(v: Double): Double {
            return v.toInt().toUInt().toDouble()
        }

        // KMP-portable byte readers. Replaces java.nio.ByteBuffer with
        // hand-rolled little-endian decoders. Endian-explicit so the
        // semantics match the original ByteBuffer.LITTLE_ENDIAN/BIG_ENDIAN
        // calls from the JVM impl.

        public fun bytesToFloat32LE(bytes: Uint8Array): Double {
            val b = bytes.buffer.asByteArray()
            val bits = (b[0].toInt() and 0xFF) or
                    ((b[1].toInt() and 0xFF) shl 8) or
                    ((b[2].toInt() and 0xFF) shl 16) or
                    ((b[3].toInt() and 0xFF) shl 24)
            return Float.fromBits(bits).toDouble()
        }

        public fun bytesToFloat64LE(bytes: Uint8Array): Double {
            val b = bytes.buffer.asByteArray()
            var bits = 0L
            for (i in 0 until 8) bits = bits or ((b[i].toLong() and 0xFF) shl (i * 8))
            return Double.fromBits(bits)
        }

        // The JVM impl reads a 4-byte int (not 8); preserved verbatim.
        fun bytesToInt64LE(bytes: Uint8Array): Double {
            val b = bytes.buffer.asByteArray()
            val v = (b[0].toInt() and 0xFF) or
                    ((b[1].toInt() and 0xFF) shl 8) or
                    ((b[2].toInt() and 0xFF) shl 16) or
                    ((b[3].toInt() and 0xFF) shl 24)
            return v.toDouble()
        }

        fun float32BEToBytes(v: Double): Uint8Array {
            val bits = v.toFloat().toRawBits()
            val out = ByteArray(4)
            out[0] = ((bits ushr 24) and 0xFF).toByte()
            out[1] = ((bits ushr 16) and 0xFF).toByte()
            out[2] = ((bits ushr 8) and 0xFF).toByte()
            out[3] = (bits and 0xFF).toByte()
            return Uint8Array(out.toUByteArray())
        }
    }
}

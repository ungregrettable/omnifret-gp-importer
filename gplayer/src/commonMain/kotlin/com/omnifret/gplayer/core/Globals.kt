@file:Suppress("NOTHING_TO_INLINE")

package com.omnifret.gplayer.core

import com.omnifret.gplayer.collections.ArrayListWithRemoveRange
import com.omnifret.gplayer.collections.List
import com.omnifret.gplayer.collections.MapEntry
import com.omnifret.gplayer.core.ecmaScript.ArrayBuffer
import com.omnifret.gplayer.core.ecmaScript.RegExp
import com.omnifret.gplayer.core.ecmaScript.Set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@ExperimentalUnsignedTypes
internal fun UByteArray.decodeToFloatArray(): FloatArray {
    // Little-endian IEEE-754 float32 decode. The TS source assumes the JS
    // platform's typed array view; we decode manually so commonMain has
    // no java.nio dependency.
    val bytes = this.toByteArray()
    val count = bytes.size / 4
    val out = FloatArray(count)
    for (i in 0 until count) {
        val b0 = bytes[i * 4].toInt() and 0xFF
        val b1 = bytes[i * 4 + 1].toInt() and 0xFF
        val b2 = bytes[i * 4 + 2].toInt() and 0xFF
        val b3 = bytes[i * 4 + 3].toInt() and 0xFF
        val bits = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        out[i] = Float.fromBits(bits)
    }
    return out
}

@ExperimentalUnsignedTypes
internal fun UByteArray.decodeToDoubleArray(): DoubleArray {
    val bytes = this.toByteArray()
    val count = bytes.size / 8
    val out = DoubleArray(count)
    for (i in 0 until count) {
        var bits = 0L
        for (j in 0 until 8) {
            bits = bits or ((bytes[i * 8 + j].toLong() and 0xFF) shl (j * 8))
        }
        out[i] = Double.fromBits(bits)
    }
    return out
}

@ExperimentalUnsignedTypes
internal fun UByteArray.decodeToString(encoding: String): String {
    val normalised = encoding.lowercase().replace("-", "").replace("_", "")
    return when (normalised) {
        "utf8" -> this.toByteArray().decodeToString()
        "ascii", "usascii" -> this.toByteArray().decodeToString()
        "latin1", "iso88591", "windows1252", "cp1252" -> {
            // Latin-1: every byte maps to the same code point.
            buildString(this.size) {
                for (b in this@decodeToString) append(b.toInt().toChar())
            }
        }
        "utf16le" -> {
            val sb = StringBuilder(this.size / 2)
            var i = 0
            while (i + 1 < this.size) {
                val code = (this[i].toInt() and 0xFF) or ((this[i + 1].toInt() and 0xFF) shl 8)
                sb.append(code.toChar())
                i += 2
            }
            sb.toString()
        }
        else -> this.toByteArray().decodeToString()
    }
}


internal inline fun <T : Comparable<T>> List<T>.sort() {
    this.sort { a, b ->
        a.compareTo(b).toDouble()
    }
}

internal inline fun String.substr(startIndex: Double, length: Double): String {
    return this.substring(startIndex.toInt(), (startIndex + length).toInt())
}

internal inline fun String.substr(startIndex: Double): String {
    return this.substring(startIndex.toInt())
}

internal inline fun String.splitBy(separator: String): List<String> {
    return List(this.split(separator))
}

internal inline fun String.replace(pattern: RegExp, replacement: String): String {
    return pattern.replace(this, replacement)
}

internal fun String.replace(
    pattern: RegExp,
    replacement: (match: String, group1: String) -> String
): String {
    return pattern.replace(this, replacement)
}
internal fun String.replace(
    pattern: RegExp,
    replacement: (match: String) -> String
): String {
    return pattern.replace(this, replacement)
}

internal inline fun String.indexOfInDouble(item: String): Double {
    return this.indexOf(item).toDouble()
}

internal inline fun String.indexOfInDouble(item: String, startIndex: Double): Double {
    return this.indexOf(item, startIndex.toInt()).toDouble()
}

internal fun Double.toInvariantString(base: Double): String {
    return this.toInt().toString(base.toInt())
}

internal fun Double.toInvariantString(): String {
    val integerPart = this.toLong()
    val fractionalPart = this - integerPart
    if (fractionalPart > 0.0000001 || fractionalPart < -0.0000001) {
        return formatDoubleInvariant(this)
    }
    return integerPart.toString()
}

// Shortest "non-scientific" decimal rendering with `.` as separator, no
// grouping, trailing zeros removed. Replaces JVM's DecimalFormat which is
// not available in commonMain. Good enough for engraving-coordinate
// serialisation, which is the only invocation site at scale.
private fun formatDoubleInvariant(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val raw = value.toString()
    // Kotlin/JVM's Double.toString may emit scientific notation
    // ("1.0E-5"); expand if so.
    val eIdx = raw.indexOf('E', ignoreCase = true)
    val plain = if (eIdx == -1) raw else expandScientific(raw, eIdx)
    return trimTrailingZeros(plain)
}

private fun expandScientific(raw: String, eIdx: Int): String {
    val mantissa = raw.substring(0, eIdx)
    val exp = raw.substring(eIdx + 1).toInt()
    val negative = mantissa.startsWith('-')
    val abs = if (negative) mantissa.substring(1) else mantissa
    val dotIdx = abs.indexOf('.')
    val digits = if (dotIdx == -1) abs else abs.substring(0, dotIdx) + abs.substring(dotIdx + 1)
    val pointPos = (if (dotIdx == -1) abs.length else dotIdx) + exp
    val sb = StringBuilder()
    if (negative) sb.append('-')
    when {
        pointPos <= 0 -> {
            sb.append("0.")
            repeat(-pointPos) { sb.append('0') }
            sb.append(digits)
        }
        pointPos >= digits.length -> {
            sb.append(digits)
            repeat(pointPos - digits.length) { sb.append('0') }
        }
        else -> {
            sb.append(digits.substring(0, pointPos))
            sb.append('.')
            sb.append(digits.substring(pointPos))
        }
    }
    return sb.toString()
}

private fun trimTrailingZeros(s: String): String {
    if (!s.contains('.')) return s
    var end = s.length
    while (end > 0 && s[end - 1] == '0') end--
    if (end > 0 && s[end - 1] == '.') end--
    return s.substring(0, end)
}

internal inline fun IGPlayerEnum.toInvariantString(): String {
    return this.value.toString()
}

internal fun Double.toFixed(decimals: Double): String {
    val n = decimals.toInt()
    val factor = pow10(n)
    val rounded = kotlin.math.round(this * factor) / factor
    val raw = formatDoubleInvariant(rounded)
    val dot = raw.indexOf('.')
    val sb = StringBuilder()
    if (dot == -1) {
        sb.append(raw)
        if (n > 0) {
            sb.append('.')
            repeat(n) { sb.append('0') }
        }
    } else {
        val have = raw.length - dot - 1
        sb.append(raw)
        if (have < n) repeat(n - have) { sb.append('0') }
        else if (have > n) sb.setLength(dot + 1 + n)
    }
    return sb.toString()
}

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10.0 }
    return r
}

internal inline fun String.lastIndexOfInDouble(item: String): Double {
    return this.lastIndexOf(item).toDouble()
}

internal inline operator fun Double.plus(str: String): String {
    return this.toInvariantString() + str
}

internal inline fun String.charAt(index: Double): String {
    return this.substring(index.toInt(), index.toInt() + 1)
}

internal inline fun String.charCodeAt(index: Int): Double {
    return this[index].code.toDouble()
}

internal inline fun String.charCodeAt(index: Double): Double {
    return this[index.toInt()].code.toDouble()
}

internal inline fun String.split(delimiter: String): List<String> {
    return List(this.split(delimiters = arrayOf(delimiter), ignoreCase = false, limit = 0))
}

internal inline fun String.substring(startIndex: Double, endIndex: Double): String {
    return this.substring(startIndex.toInt(), endIndex.toInt())
}

internal inline operator fun String.get(index: Double): Char {
    return this[index.toInt()]
}

internal inline fun String.substring(startIndex: Double): String {
    return this.substring(startIndex.toInt())
}

internal inline fun String.replaceAll(before: String, after: String): String {
    return this.replace(before, after)
}

internal inline fun String.replaceAll(search: RegExp, after: String): String {
    return this.replace(search, after)
}

internal inline fun String.replaceAll(
    search: RegExp,
    noinline replacement: (match: String) -> String
): String {
    return this.replace(search, replacement)
}

internal inline fun IGPlayerEnum.toDouble(): Double {
    return this.value.toDouble()
}

internal inline fun IGPlayerEnum?.toDouble(): Double? {
    return this?.value.toDouble()
}

internal inline fun IGPlayerEnum.toInt(): Int {
    return this.value
}

internal inline fun IGPlayerEnum?.toInt(): Int? {
    return this?.value
}

internal inline fun Double.toTemplate(): String {
    return this.toInvariantString()
}

internal inline fun Double?.toTemplate(): String {
    return this?.toInvariantString() ?: ""
}

internal fun Any?.toTemplate(): String = when (this) {
    null -> ""
    is IGPlayerEnum -> this.toInvariantString()
    is Double -> this.toTemplate()
    else -> this.toString()
}

internal fun Any?.toDouble(): Double {
    if (this is Double) {
        return this
    }
    if (this == null) {
        throw ClassCastException("Cannot cast null to double")
    }
    throw ClassCastException("Cannot cast ${this::class.simpleName} to double")
}

internal fun Any?.toLong(): Long {
    if (this is Long) {
        return this
    }
    if (this == null) {
        throw ClassCastException("Cannot cast null to long")
    }
    throw ClassCastException("Cannot cast ${this::class.simpleName} to long")
}

internal inline fun Int?.toDouble(): Double? {
    return this?.toDouble()
}

internal fun String.toDoubleOrNaN(): Double {
    return this.trim().toDoubleOrNull() ?: Double.NaN
}

internal fun String.toIntOrNaN(): Double {
    return this.trim().toIntOrNull()?.toDouble() ?: Double.NaN
}

internal fun String.toIntOrNaN(radix: Double): Double {
    return this.trim().toIntOrNull(radix.toInt())?.toDouble() ?: Double.NaN
}


internal class Globals {
    companion object {
        val console = Console()

        inline fun setImmediate(action: () -> Unit) {
            action()
        }

        inline fun setTimeout(noinline action: () -> Unit, millis: Double): Deferred<Unit> {
            @Suppress("OPT_IN_USAGE")
            return GlobalScope.async {
                delay(millis.toLong())
                action()
            }
        }

        val performance = Performance()
    }
}

internal class Performance {
    private val origin = TimeSource.Monotonic.markNow()
    fun now(): Double {
        return origin.elapsedNow().inWholeMicroseconds.toDouble() / 1000.0
    }
}

internal fun List<Char>.toCharArray(): CharArray {
    val result = CharArray(length.toInt())
    var index = 0
    for (element in this) {
        result[index++] = element
    }
    return result
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.then(callback: (T) -> Unit): Deferred<T> {
    this.invokeOnCompletion {
        if (it == null) {
            callback(this.getCompleted())
        }
    }
    return this
}

internal fun <T> Deferred<T>.catch(callback: (com.omnifret.gplayer.core.ecmaScript.Error) -> Unit): Deferred<T> {
    this.invokeOnCompletion {
        if (it != null) {
            callback(it)
        }
    }
    return this
}

@OptIn(ExperimentalUnsignedTypes::class)
internal val ArrayBuffer.byteLength: Int
    get() = this.size

internal fun String.repeat(count: Double): String {
    return this.repeat(count.toInt())
}

internal fun String.padStart(length: Double, pad: String): String {
    return this.padStart(length.toInt(), pad[0])
}

internal val Throwable.stack: String
    get() = this.stackTraceToString()

internal inline fun <reified T> List<T>.spread(): Array<T> {
    return _data.toTypedArray()
}

internal inline fun <reified T> List<T?>.filterNotNull(): List<T> {
    return List(_data.filterNotNullTo(ArrayListWithRemoveRange()))
}

internal inline fun <reified TKey, reified TValue, reified TResult> List<MapEntry<TKey, TValue>>.map(
    func: (v: ArrayTuple<TKey, TValue>) -> TResult
): List<TResult> {
    return List(
        _data.map { func(ArrayTuple(it.key, it.value)) }.toCollection(
            ArrayListWithRemoveRange()
        )
    )
}

internal inline fun Set<Double>.spread(): DoubleArray {
    val empty = DoubleArray(this.size.toInt())
    for ((i, v) in this.withIndex()) {
        empty[i] = v
    }
    return empty
}

internal inline fun <reified T> Set<T>.spread(): kotlin.Array<T> {
    val empty = arrayOfNulls<T>(this.size.toInt())
    for ((i, v) in this.withIndex()) {
        empty[i] = v
    }
    @Suppress("UNCHECKED_CAST")
    return empty as kotlin.Array<T>
}

internal class IteratorIterable<T>(private val iterator: Iterator<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> {
        return iterator
    }
}

internal inline fun Iterator<Double>.spread(): DoubleArray {
    return IteratorIterable(this).toList().toDoubleArray()
}

internal inline fun <reified T> Iterator<T>.spread(): kotlin.Array<T> {
    return IteratorIterable(this).toList().toTypedArray()
}

internal inline fun <reified T> List<T>.concat(other: Iterable<T>): List<T> {
    val copy = this.slice()
    copy.push(other)
    return copy
}

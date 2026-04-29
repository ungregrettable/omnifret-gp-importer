package com.omnifret.gplayer.core

import com.omnifret.gplayer.GPlayerError
import com.omnifret.gplayer.GPlayerErrorType
import com.omnifret.gplayer.core.ecmaScript.RegExp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

typealias Disposable = AutoCloseable

internal class TypeHelper {
    companion object {
        public fun createRegex(pattern: String, flags: String): RegExp {
            return RegExp(pattern, flags)
        }

        @ExperimentalContracts
        @ExperimentalUnsignedTypes
        public inline fun <reified T : Enum<T>> parseEnum(
            value: String,
            @Suppress("UNUSED_PARAMETER") type: KClass<T>
        ): T {
            return parseEnum(value, enumValues<T>())
        }

        @ExperimentalContracts
        @ExperimentalUnsignedTypes
        public fun <T : Enum<T>> parseEnum(value: String, values: Array<T>): T {
            val valueLower = value.lowercase()
            for (e in values) {
                if (valueLower.equals(e.name, true)) {
                    return e
                }
            }
            throw GPlayerError(GPlayerErrorType.General, "Could not parse enum value '$value'")
        }

        @ExperimentalContracts
        public fun isTruthy(s: String?): Boolean {
            contract { returns(true) implies (s != null) }
            return s != null && s.isNotEmpty()
        }

        public fun isTruthy(b: Boolean?): Boolean {
            return b != null && b
        }

        @ExperimentalContracts
        public fun isTruthy(s: Any?): Boolean {
            contract { returns(true) implies (s != null) }
            return s != null
        }

        @ExperimentalContracts
        public fun isTruthy(s: Double): Boolean {
            return !s.isNaN() && s != 0.0
        }

        @ExperimentalUnsignedTypes
        public fun typeOf(s: Any?): String {
            return when (s) {
                is String -> "string"
                is Boolean -> "boolean"
                is Byte,
                is Short,
                is Int,
                is Long,
                is UByte,
                is UShort,
                is UInt,
                is ULong,
                is Float,
                is Double,
                is IGPlayerEnum -> "number"

                null -> "undefined"
                else -> "object"
            }
        }

        @Suppress("NOTHING_TO_INLINE")
        public inline fun <T> setInitializer(vararg values: T): Iterable<T> {
            return values.asIterable()
        }

        @Suppress("NOTHING_TO_INLINE")
        public inline fun <T> mapInitializer(vararg values: T): Iterable<T> {
            return values.asIterable();
        }

        public fun <T> suspendToDeferred(block: suspend CoroutineScope.() -> T): kotlinx.coroutines.Deferred<T> {
            @Suppress("OPT_IN_USAGE")
            return GlobalScope.async {
                block()
            }
        }

        @kotlin.jvm.JvmName("createPromiseWithValue")
        public fun <T> createPromise(action: (resolve: (T) -> Unit, reject: (Any) -> Unit) -> Unit): kotlinx.coroutines.Deferred<T> {
            val d = CompletableDeferred<T>()
            @Suppress("DeferredResultUnused", "OPT_IN_USAGE")
            GlobalScope.async {
                action(
                    { d.complete(it) },
                    {
                        when (it) {
                            is com.omnifret.gplayer.core.ecmaScript.Error -> {
                                d.completeExceptionally(it)
                            }

                            else -> {
                                d.completeExceptionally(com.omnifret.gplayer.core.ecmaScript.Error("Promise rejected with: $it"))
                            }
                        }
                    }
                )
            }
            return d
        }

        public fun createPromise(action: (resolve: () -> Unit, reject: (Any) -> Unit) -> Unit): kotlinx.coroutines.Deferred<Unit> {
            val d = CompletableDeferred<Unit>()
            @Suppress("DeferredResultUnused", "OPT_IN_USAGE")
            GlobalScope.async {
                action(
                    { d.complete(Unit) },
                    {
                        when (it) {
                            is com.omnifret.gplayer.core.ecmaScript.Error -> {
                                d.completeExceptionally(it)
                            }

                            else -> {
                                d.completeExceptionally(com.omnifret.gplayer.core.ecmaScript.Error("Promise rejected with: $it"))
                            }
                        }
                    }
                )
            }
            return d
        }

        fun unknownToNumber(v: Any?): Double {
            when (v) {
                null -> {
                    throw ClassCastException("Cannot cast null to number")
                }

                is Int -> {
                    return v.toDouble()
                }

                is Double -> {
                    return v
                }

                is IGPlayerEnum -> {
                    return v.toDouble()
                }

                else -> {
                    throw ClassCastException(
                        "Cannot cast ${v::class.simpleName ?: "?"} to number"
                    )
                }
            }
        }
    }
}

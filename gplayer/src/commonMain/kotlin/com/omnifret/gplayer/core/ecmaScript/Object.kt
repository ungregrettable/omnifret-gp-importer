package com.omnifret.gplayer.core.ecmaScript

import com.omnifret.gplayer.collections.List
import com.omnifret.gplayer.core.ArrayTuple
import com.omnifret.gplayer.core.IGPlayerEnum
import com.omnifret.gplayer.core.IGPlayerEnumCompanion
import com.omnifret.gplayer.core.IArrayTuple
import com.omnifret.gplayer.core.toDouble
import kotlin.reflect.KClass

internal class Object {
    companion object {
        fun <TKey, TValue> entries(v: Record<TKey, TValue>): List<IArrayTuple<String, TValue>> {
            val entries = List<IArrayTuple<String, Any>>()

            for (item in v) {
                val value = item.value as Any?
                if (value != null) {
                    entries.push(ArrayTuple(item.key.toString(), value))
                }
            }

            @Suppress("UNCHECKED_CAST")
            return entries as List<IArrayTuple<String, TValue>>
        }

        // Caller is expected to register the companion for any enum that
        // needs reflection-style iteration. The transpiled call sites we
        // see (Duration, MusicFontSymbol) all hit the Record overload or
        // the KClass overload below; if the transpiler ever emits an
        // entries(POJO) call we'll need to add a code-generated lookup.
        private val enumCompanions = HashMap<KClass<*>, IGPlayerEnumCompanion<*>?>()

        fun registerEnum(kClass: KClass<*>, companion: IGPlayerEnumCompanion<*>) {
            enumCompanions[kClass] = companion
        }

        fun entries(v: Any?): List<IArrayTuple<String, Any>> {
            val entries = List<IArrayTuple<String, Any>>()
            if (v == null) return entries

            if (v is KClass<*>) {
                val comp = enumCompanions[v]
                if (comp != null) {
                    for (e in comp.values) {
                        entries.push(
                            ArrayTuple(
                                (e as IGPlayerEnum).toString(),
                                e.value.toDouble()
                            )
                        )
                    }
                }
                return entries
            }

            // Java-reflection path (entries on a POJO) is not portable to
            // commonMain. The transpiled the library does not exercise this
            // path at runtime, but it must compile since some enum lookup
            // utilities reference it. Throw if invoked.
            throw IllegalStateException(
                "Object.entries on a POJO is not supported in the KMP port; " +
                "use the Record or KClass<Enum> overload."
            )
        }

        inline fun <reified T : Enum<T>> values(@Suppress("UNUSED_PARAMETER") type: KClass<T>): List<Any> {
            return List(*enumValues<T>()).map<Any> { v ->
                (v as IGPlayerEnum).value.toDouble()
            }
        }
    }
}

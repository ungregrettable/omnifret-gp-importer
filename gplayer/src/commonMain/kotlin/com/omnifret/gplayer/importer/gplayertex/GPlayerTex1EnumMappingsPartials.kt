package com.omnifret.gplayer.importer.gplayertex

import com.omnifret.gplayer.core.IGPlayerEnum
import com.omnifret.gplayer.core.IGPlayerEnumCompanion
import kotlin.reflect.KClass

// KMP-PORT: the upstream Android impl uses java.lang.reflect to find the
// `Companion` static field on a Kotlin enum. That doesn't port to
// Kotlin/Native. Instead, the importer registers each enum's companion
// up-front by calling `register(...)` on this object during static init.
// Coverage check: every enum the GPlayerTex importer parses has a
// `IGPlayerEnumCompanion` declared; the original reflection just looked
// it up dynamically.
internal class GPlayerTex1EnumMappingsPartials {
    companion object {
        private val factories = HashMap<KClass<*>, (Double) -> IGPlayerEnum>()

        fun register(klass: KClass<*>, companion: IGPlayerEnumCompanion<*>) {
            factories[klass] = { v -> companion.fromValue(v) as IGPlayerEnum }
        }

        fun <T> _toEnum(type: Any?, value: Double): T where T : IGPlayerEnum {
            val clz = type as KClass<*>
            val factory = factories[clz]
                ?: throw IllegalArgumentException(
                    "No enum factory registered for ${clz.simpleName}; call " +
                        "GPlayerTex1EnumMappingsPartials.register() at startup."
                )
            @Suppress("UNCHECKED_CAST")
            return factory(value) as T
        }
    }
}

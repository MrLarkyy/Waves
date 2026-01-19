package gg.aquatic.waves.editor

import gg.aquatic.common.getSectionList
import gg.aquatic.common.toMMString
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

interface ValueSerializer<T> {
    /**
     * Writes the value to the given section at the specified path.
     */
    fun serialize(section: ConfigurationSection, path: String, value: T)

    /**
     * Reads the value from the given section at the specified path.
     */
    fun deserialize(section: ConfigurationSection, path: String): T

    object MaterialSerializer : ValueSerializer<Material> {
        override fun serialize(section: ConfigurationSection, path: String, value: Material) {
            section.set(path, value.name)
        }

        override fun deserialize(section: ConfigurationSection, path: String): Material {
            return Material.matchMaterial(section.getString(path) ?: "STONE") ?: Material.STONE
        }
    }

    class IntSerializer(private val default: Int = 1) : ValueSerializer<Int> {
        override fun serialize(section: ConfigurationSection, path: String, value: Int) {
            section.set(path, value)
        }

        override fun deserialize(section: ConfigurationSection, path: String): Int {
            return section.getInt(path, default)
        }
    }

    class Simple<T>(
        private val default: T? = null,
        private val encode: (Any) -> T?,
        private val decode: (T) -> Any? = { it }
    ) : ValueSerializer<T> {
        override fun serialize(section: ConfigurationSection, path: String, value: T) {
            section.set(path, decode(value))
        }

        override fun deserialize(section: ConfigurationSection, path: String): T {
            val raw = section.get(path) ?: return default ?: throw IllegalStateException("Missing $path")
            return encode(raw) ?: default ?: throw IllegalStateException("Invalid data at $path")
        }
    }

    class ListSection<T : Configurable<T>>(
        private val factory: () -> T
    ) : ValueSerializer<MutableList<T>> {
        override fun serialize(section: ConfigurationSection, path: String, value: MutableList<T>) {
            val resultList = value.map { item ->
                val tempSection = MemoryConfiguration()
                item.serialize(tempSection)
                tempSection.getValues(false)
            }
            section.set(path, resultList.ifEmpty { null })
        }

        override fun deserialize(section: ConfigurationSection, path: String): MutableList<T> {
            val sections = section.getSectionList(path)
            return sections.map { sec ->
                factory().apply { deserialize(sec) }
            }.toMutableList()
        }
    }

    class Str(val default: String? = null) : ValueSerializer<String> {
        override fun serialize(section: ConfigurationSection, path: String, value: String) {
            section.set(path, value)
        }

        override fun deserialize(section: ConfigurationSection, path: String): String {
            return section.getString(path) ?: default ?: throw IllegalStateException("Missing value for $path")
        }
    }

    class EnumSerializer<T : Enum<T>>(private val clazz: Class<T>) : ValueSerializer<T> {
        override fun serialize(section: ConfigurationSection, path: String, value: T) {
            section.set(path, value.name)
        }

        override fun deserialize(section: ConfigurationSection, path: String): T {
            val name = section.getString(path) ?: clazz.enumConstants[0].name
            return java.lang.Enum.valueOf(clazz, name)
        }
    }
}

object Serializers {
    val MATERIAL = ValueSerializer.Simple(Material.STONE, encode = { Material.matchMaterial(it.toString()) }, decode = { it.toString() })
    val INT = ValueSerializer.Simple(1, encode = { it.toString().toIntOrNull() ?: 1 })
    val FLOAT = ValueSerializer.Simple(1f, encode = { it.toString().toFloatOrNull() ?: 1f })
    val DOUBLE = ValueSerializer.Simple(1.0, encode = { it.toString().toDoubleOrNull() ?: 1.0 })
    val LONG = ValueSerializer.Simple(1L, encode = { it.toString().toLongOrNull() ?: 1L })
    val STRING = ValueSerializer.Simple("", encode = { it.toString() })
    val COMPONENT = ValueSerializer.Simple(
        Component.empty(),
        { MiniMessage.miniMessage().deserialize(it.toString()) },
        { it.toMMString() })
    val BOOLEAN = ValueSerializer.Simple(false, encode = { it.toString().toBoolean() })
    val SOUND = ValueSerializer.Simple(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, encode = { Registry.SOUNDS.get(Key.key(it.toString())) }, decode = { Registry.SOUNDS.getKey(it)?.toString() })

    fun <T : Enum<T>> enum(clazz: Class<T>) = ValueSerializer.EnumSerializer(clazz)
}
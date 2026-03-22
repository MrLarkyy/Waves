package gg.aquatic.waves.editor.serialize

import gg.aquatic.common.getSectionList
import gg.aquatic.waves.editor.Configurable
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

interface ValueSerializer<T> {

    companion object

    /**
     * Writes the value to the given section at the specified path.
     */
    fun serialize(section: ConfigurationSection, path: String, value: T)

    /**
     * Reads the value from the given section at the specified path.
     */
    fun deserialize(section: ConfigurationSection, path: String): T

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


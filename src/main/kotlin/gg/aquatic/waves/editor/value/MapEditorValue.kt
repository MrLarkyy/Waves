package gg.aquatic.waves.editor.value

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.ValueSerializer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MapEditorValue<T>(
    override val key: String,
    override var value: MutableList<EditorValue<T>>,
    val addButtonClick: (player: Player, accept: (EditorValue<T>?) -> Unit) -> Unit,
    private val iconFactory: (Map<String, T>) -> ItemStack,
    private val openMapGui: (Player, MapEditorValue<T>, () -> Unit) -> Unit,
    override val visibleIf: () -> Boolean = { true },
    override val defaultValue: MutableList<EditorValue<T>>? = null,
    private val elementFactory: (ConfigurationSection) -> EditorValue<T>
) : EditorValue<MutableList<EditorValue<T>>> {

    override val serializer: ValueSerializer<MutableList<EditorValue<T>>> = Serializer(elementFactory)

    override fun getDisplayItem(): ItemStack {
        val map = value.associate { it.key to it.value }
        return iconFactory(map)
    }

    override fun onClick(player: Player, clickType: ButtonType, updateParent: () -> Unit) {
        openMapGui(player, this, updateParent)
    }

    override fun clone(): MapEditorValue<T> {
        return MapEditorValue(
            key, value.map { it.clone() }.toMutableList(),
            addButtonClick, iconFactory, openMapGui,
            visibleIf, defaultValue, elementFactory
        )
    }

    val map: Map<String, T>
        get() = value.associate { it.key to it.value }

    class Serializer<T>(
        private val elementFactory: (ConfigurationSection) -> EditorValue<T>
    ) : ValueSerializer<MutableList<EditorValue<T>>> {

        override fun serialize(section: ConfigurationSection, path: String, value: MutableList<EditorValue<T>>) {
            val subSection = section.getConfigurationSection(path) ?: section.createSection(path)
            value.forEach { editor ->
                editor.save(subSection)
            }
        }

        override fun deserialize(section: ConfigurationSection, path: String): MutableList<EditorValue<T>> {
            val subSection = section.getConfigurationSection(path) ?: return mutableListOf()
            return subSection.getKeys(false).map { key ->
                val elementSection = subSection.getConfigurationSection(key)!!
                elementFactory(elementSection).apply { load(elementSection) }
            }.toMutableList()
        }
    }
}

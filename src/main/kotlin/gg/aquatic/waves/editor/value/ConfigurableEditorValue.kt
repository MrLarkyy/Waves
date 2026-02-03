package gg.aquatic.waves.editor.value

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.EditorHandler.getEditorContext
import gg.aquatic.waves.editor.ValueSerializer
import gg.aquatic.waves.editor.ui.EditorMenuProvider
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ConfigurableEditorValue<T : Configurable<T>>(
    override val key: String,
    override var value: T,
    private val iconFactory: (T) -> ItemStack,
    override val visibleIf: () -> Boolean = { true },
    override val defaultValue: T? = null
) : EditorValue<T> {

    override val serializer = object : ValueSerializer<T> {
        override fun serialize(section: ConfigurationSection, path: String, value: T) {
            val sub = section.getConfigurationSection(path) ?: section.createSection(path)
            value.serialize(sub)
        }

        override fun deserialize(section: ConfigurationSection, path: String): T {
            val sub = section.getConfigurationSection(path) ?: MemoryConfiguration()
            val instance = value.copy() // Start with a copy or fresh instance
            instance.deserialize(sub)
            return instance
        }
    }

    override fun getDisplayItem(): ItemStack = iconFactory(value)

    override suspend fun onClick(player: Player, clickType: ButtonType, updateParent: suspend () -> Unit) {
        val context = player.getEditorContext() ?: return
        context.navigate {
            EditorMenuProvider.openValueEditor(
                context = context,
                title = Component.text("Editing: $key"),
                values = value.getEditorValues(),
                onSave = { updateParent() }
            )
        }
    }

    override fun clone(): EditorValue<T> {
        return ConfigurableEditorValue(key, value.copy(), iconFactory, visibleIf, defaultValue)
    }

    override fun save(section: ConfigurationSection) {
        if (!visibleIf()) return
        // We override save to ensure the nested structure is respected 
        // by always serializing into a sub-section of the provided key
        val sub = section.getConfigurationSection(key) ?: section.createSection(key)
        value.serialize(sub)
    }

    override fun load(section: ConfigurationSection) {
        val sub = section.getConfigurationSection(key) ?: return
        value.deserialize(sub)
    }
}

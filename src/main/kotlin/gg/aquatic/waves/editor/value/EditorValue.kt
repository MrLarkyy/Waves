package gg.aquatic.waves.editor.value

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.ValueSerializer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface EditorValue<T> {
    val key: String // e.g., "material", "amount", or "0" (if in a list)
    val serializer: ValueSerializer<T>
    // The actual data being edited
    var value: T
    val visibleIf: () -> Boolean
    val defaultValue: T?

    // 1. What to show in the GUI
    fun getDisplayItem(): ItemStack
    
    // 2. What happens when the player clicks this icon
    fun onClick(player: Player, clickType: ButtonType, updateParent: () -> Unit)
    
    // 3. Return a deep-copied version of this editor state
    fun clone(): EditorValue<T>

    fun save(section: ConfigurationSection) {
        if (!visibleIf()) return
        serializer.serialize(section, key, value)
    }

    fun load(section: ConfigurationSection) {
        if (section.contains(key)) {
            value = serializer.deserialize(section, key)
        } else if (defaultValue != null) {
            value = defaultValue ?: throw IllegalStateException("Missing value for $key")
        }
    }
}

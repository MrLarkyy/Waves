package gg.aquatic.waves.editor.value

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.editor.ValueSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SimpleEditorValue<T>(
    override val key: String,
    override var value: T,
    private val displayIcon: (T) -> ItemStack,
    private val clickHandler: EditorClickHandler<T>,
    private val cloning: (T) -> T = { it },
    override val serializer: ValueSerializer<T>,
    override val visibleIf: () -> Boolean = { true },
    override val defaultValue: T? = null,
) : EditorValue<T> {

    override fun getDisplayItem(): ItemStack = displayIcon(value)

    override fun onClick(player: Player, clickType: ButtonType, updateParent: () -> Unit) {
        clickHandler.handle(player, value, clickType) { newValue ->
            if (newValue != null) {
                value = newValue
            }
            updateParent() // Refreshes the GUI
        }
    }

    override fun clone(): SimpleEditorValue<T> {
        return SimpleEditorValue(
            key,
            cloning(value),
            displayIcon,
            clickHandler,
            cloning,
            serializer,
            visibleIf,
            defaultValue
        )
    }
}

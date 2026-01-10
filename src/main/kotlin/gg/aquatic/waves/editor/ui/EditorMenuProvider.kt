package gg.aquatic.waves.editor.ui

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.value.EditorValue
import net.kyori.adventure.text.Component
import org.bukkit.Material

object EditorMenuProvider {

    suspend fun openValueEditor(
        context: EditorContext,
        title: Component,
        values: List<EditorValue<*>>,
        onSave: () -> Unit
    ) {
        context.player.createMenu(title, InventoryType.GENERIC9X6) {
            // Map values to slots (simplistic example: 10-16, 19-25, etc.)
            val slots = (10..16) + (19..25) + (28..34)

            values.forEachIndexed { index, editorValue ->
                if (index >= slots.size) return@forEachIndexed
                if (!editorValue.visibleIf()) return@forEachIndexed

                button("val_${editorValue.key}", slots[index]) {
                    item = editorValue.getDisplayItem()
                    onClick { event ->
                        // Handle the logic when a value is clicked
                        editorValue.onClick(context.player, event.buttonType) {
                            // This is the 'updateParent' callback
                            // Refresh this menu when a child value changes
                            KMenuCtx.launch {
                                openValueEditor(context, title, values, onSave)
                            }
                        }
                    }
                }
            }

            // Back/Save Button
            button("back_button", 49) {
                item = stackedItem(Material.ARROW) {
                    this.displayName = Component.text("Back")
                }.getItem()
                onClick {
                    onSave()
                    context.goBack()
                }
            }
        }.open(context.player)
    }
}
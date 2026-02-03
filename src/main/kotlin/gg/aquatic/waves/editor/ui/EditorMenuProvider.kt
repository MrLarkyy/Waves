package gg.aquatic.waves.editor.ui

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.value.EditorValue
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material

object EditorMenuProvider {

    suspend fun openValueEditor(
        context: EditorContext,
        title: Component,
        values: List<EditorValue<*>>,
        onSave: suspend () -> Unit
    ) {
        context.player.createMenu(title, InventoryType.GENERIC9X6) {
            val slots = (10..16) + (19..25) + (28..34)

            // Value Buttons
            values.filter { it.visibleIf() }.forEachIndexed { index, editorValue ->
                if (index >= slots.size) return@forEachIndexed

                button("val_${editorValue.key}", slots[index]) {
                    item = editorValue.getDisplayItem()
                    onClick { event ->
                        withContext(BukkitCtx.ofEntity(context.player)) {
                            editorValue.onClick(context.player, event.buttonType) {
                                context.refresh()
                            }
                        }
                    }
                }
            }

            // SAVE BUTTON - Always visible in Configurable editors
            button("save_changes", 48) {
                item = stackedItem(Material.LIME_DYE) {
                    displayName = Component.text("Save Changes")
                    lore.add(Component.text("Click to apply all changes."))
                }.getItem()
                onClick {
                    onSave()
                }
            }

            // BACK BUTTON - Only visible if there is a previous menu to return to
            if (context.path.size > 1) {
                button("back_button", 49) {
                    item = stackedItem(Material.ARROW) {
                        displayName = Component.text("Go Back")
                    }.getItem()
                    onClick {
                        context.goBack()
                    }
                }
            }
        }.open(context.player)
    }
}

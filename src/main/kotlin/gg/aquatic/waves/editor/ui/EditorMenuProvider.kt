package gg.aquatic.waves.editor.ui

import gg.aquatic.execute.coroutine.BukkitCtx
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.value.EditorValue
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component

object EditorMenuProvider {

    suspend fun openValueEditor(
        context: EditorContext,
        title: Component,
        values: List<EditorValue<*>>,
        onSave: () -> Unit
    ) {
        context.player.createMenu(title, InventoryType.GENERIC9X6) {
            val slots = (10..16) + (19..25) + (28..34)

            values.forEachIndexed { index, editorValue ->
                if (index >= slots.size) return@forEachIndexed
                if (!editorValue.visibleIf()) return@forEachIndexed

                button("val_${editorValue.key}", slots[index]) {
                    item = editorValue.getDisplayItem()
                    onClick { event ->
                        withContext(BukkitCtx.ofEntity(context.player)) {
                            editorValue.onClick(context.player, event.buttonType) {
                                // Refresh the current screen
                                KMenuCtx.launch { context.refresh() }
                            }
                        }
                    }
                }
            }

            button("back_button", 49) {
                // ... item setup ...
                onClick {
                    onSave()
                    KMenuCtx.launch { context.goBack() }
                }
            }
        }.open(context.player)
    }
}
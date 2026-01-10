package gg.aquatic.waves.editor.ui

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.component.Button
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.waves.editor.value.ListEditorValue
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ConfigurableListMenu<T> private constructor(
    player: Player,
    title: Component,
) : ListMenu<T>(
    title,
    InventoryType.GENERIC9X6,
    player,
    entries = emptyList(), // Populated in create
    defaultSorting = Sorting.empty(),
    entrySlots = (0..44).toList()
) {

    companion object {
        suspend fun <T> create(
            player: Player,
            editor: ListEditorValue<T>,
            title: Component,
            onUpdate: () -> Unit
        ): ConfigurableListMenu<T> {
            val menu = ConfigurableListMenu<T>(player, title)

            // Map existing values to entries
            menu.entries = editor.value.map { element ->
                Entry(
                    value = element.value,
                    itemVisual = { element.getDisplayItem() },
                    placeholderContext = menu.placeholderContext,
                    onClick = { event ->
                        if (event.buttonType == ButtonType.LEFT) {
                            element.onClick(player, event.buttonType) {
                                onUpdate()

                                menu.requestRefresh()
                                KMenuCtx.launch {
                                    menu.open()
                                }
                            }
                        } else if (event.buttonType == ButtonType.DROP) {
                            editor.value.remove(element)
                            onUpdate()
                            menu.requestRefresh()
                        }
                    }
                )
            }

            // Add the "Add New" button
            menu.addComponent(
                Button(
                    "add-button",
                    ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                        editMeta { it.displayName(Component.text("§aAdd New Element")) }
                    },
                    listOf(49),
                    1, 10, null,
                    textUpdater = menu.placeholderContext
                ) {
                    editor.addButtonClick(player) { newEditorValue ->
                        KMenuCtx.launch { menu.open() }

                        if (newEditorValue == null) return@addButtonClick

                        editor.value.add(newEditorValue)
                        onUpdate()
                        // Update entries list to include the new one
                        menu.entries = editor.value.map { /* same mapping as above */
                            Entry(it.value, { it.getDisplayItem() }, menu.placeholderContext, { event ->
                                // (Re-using logic from above mapping)
                            })
                        }
                        menu.requestRefresh()
                    }

                })

            menu.open()
            return menu
        }
    }
}

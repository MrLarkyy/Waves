package gg.aquatic.waves.editor.ui

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.component.Button
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.placeholder.PlaceholderContext
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.value.EditorValue
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ConfigurableListMenu<T>(
    val context: EditorContext,
    val listValue: EditorValue<MutableList<EditorValue<T>>>,
    val addButtonClick: (Player, (EditorValue<T>?) -> Unit) -> Unit,
    val updateParent: () -> Unit
) : ListMenu<EditorValue<T>>(
    title = Component.text("Editing: ${listValue.key}"),
    type = InventoryType.GENERIC9X6,
    player = context.player,
    entries = emptyList(),
    defaultSorting = Sorting.empty(),
    entrySlots = (10..16) + (19..25) + (28..34)
) {

    init {
        rebuildEntries()
    }

    private fun rebuildEntries() {
        this.entries = listValue.value.map { editor ->
            Entry(
                value = editor,
                itemVisual = { editor.getDisplayItem() },
                placeholderContext = PlaceholderContext.privateMenu(),
                onClick = { event ->
                    if (event.buttonType == ButtonType.DROP) {
                        listValue.value.remove(editor)
                        updateParent()
                        context.refresh()
                        return@Entry
                    }

                    withContext(BukkitCtx.ofEntity(context.player)) {
                        editor.onClick(context.player, event.buttonType) {
                            KMenuCtx.launch {
                                updateParent()
                                context.refresh()
                            }
                        }
                    }
                }
            )
        }
    }

    override suspend fun open(player: Player) {
        // Correctly adding the "Add New" button using Button class and addComponent
        val addButton = Button(
            id = "add_new",
            itemstack = ItemStack(Material.NETHER_STAR),
            slots = listOf(48),
            priority = 10,
            updateEvery = -1,
            textUpdater = placeholderContext,
            onClick = {
                withContext(BukkitCtx.ofEntity(player)) {
                    addButtonClick(player) { newValue ->
                        if (newValue != null) {
                            listValue.value.add(newValue)
                            rebuildEntries()
                            requestRefresh()
                            updateParent()

                            KMenuCtx.launch { open(player) }
                        }
                    }
                }
            },
            failComponent = null
        )
        addComponent(addButton)

        // Correctly adding the "Back" button
        val backButton = Button(
            id = "back",
            itemstack = ItemStack(Material.ARROW),
            slots = listOf(49),
            priority = 10,
            updateEvery = -1,
            textUpdater = placeholderContext,
            onClick = {
                KMenuCtx.launch {
                    context.goBack()
                }
            },
            failComponent = null
        )
        addComponent(backButton)

        super.open(player)
    }
}

package gg.aquatic.waves.editor.ui

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.kmenu.menu.component.Button
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.placeholder.PlaceholderContext
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.value.EditorValue
import gg.aquatic.waves.editor.value.ListEditorValue
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ConfigurableListMenu<T>(
    val context: EditorContext,
    val listValue: ListEditorValue<T>,
    val updateParent: () -> Unit
) : ListMenu<EditorValue<T>>(
    title = Component.text("Editing: ${listValue.key}"),
    type = InventoryType.GENERIC9X6,
    player = context.player,
    entries = listValue.value.map { editor ->
        Entry(
            value = editor,
            itemVisual = { editor.getDisplayItem() },
            placeholderContext = PlaceholderContext.privateMenu(),
            onClick = { event ->
                if (event.buttonType == ButtonType.RIGHT) {
                    // Quick Delete
                    listValue.value.remove(editor)
                    updateParent()
                    // Re-open this list to refresh
                    (event.inventory as PrivateMenu).open()
                    return@Entry
                }

                // Navigate deeper into this element
                context.navigateTo(
                    backLogic = {
                        (event.inventory as PrivateMenu).open()
                    }
                ) {
                    editor.onClick(context.player, event.buttonType) {
                        // Refresh logic when the sub-editor changes something
                        updateParent()
                    }
                }
            }
        )
    },
    defaultSorting = Sorting.empty(),
    entrySlots = (10..16) + (19..25) + (28..34)
) {
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
                listValue.addButtonClick(player) { newValue ->
                    if (newValue != null) {
                        listValue.value.add(newValue)
                        updateParent()
                        KMenuCtx.launch { open(player) }
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
                // Launch in KMenuCtx since goBack is a suspend function
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

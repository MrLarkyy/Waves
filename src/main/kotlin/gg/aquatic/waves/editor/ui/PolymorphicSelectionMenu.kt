package gg.aquatic.waves.editor.ui

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.component.Button
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.PlaceholderContext
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.EditorContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PolymorphicSelectionMenu<T : Configurable<T>>(
    val context: EditorContext,
    title: Component,
    val options: Map<String, () -> T>,
    val onSelect: (T) -> Unit
) : ListMenu<String>(
    title = title,
    type = InventoryType.GENERIC9X6,
    player = context.player,
    entries = emptyList(),
    defaultSorting = Sorting.empty(),
    entrySlots = (10..16) + (19..25) + (28..34)
) {

    init {
        this.entries = options.keys.map { id ->
            Entry(
                value = id,
                itemVisual = { 
                    ItemStack(Material.PAPER).apply { 
                        editMeta { it.displayName(Component.text(id)) } 
                    } 
                },
                placeholderContext = PlaceholderContext.privateMenu(),
                onClick = { event ->
                    if (event.buttonType != ButtonType.LEFT) return@Entry
                    val factory = options[id] ?: return@Entry
                    onSelect(factory())
                }
            )
        }
    }

    override suspend fun open(player: Player) {
        val backButton = Button(
            id = "back",
            itemstack = ItemStack(Material.ARROW),
            slots = listOf(49),
            priority = 10,
            updateEvery = -1,
            textUpdater = placeholderContext,
            onClick = { context.goBack() }
        )
        addComponent(backButton)
        super.open(player)
    }
}

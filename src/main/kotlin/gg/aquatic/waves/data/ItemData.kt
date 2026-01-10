package gg.aquatic.waves.data

import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.EditorHandler.getEditorContext
import gg.aquatic.waves.editor.Serializers.COMPONENT
import gg.aquatic.waves.editor.Serializers.INT
import gg.aquatic.waves.editor.Serializers.MATERIAL
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.handlers.ListGuiHandlerImpl
import gg.aquatic.waves.editor.value.ElementBehavior
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.toMMComponent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class ItemData(
    initialMaterial: Material = Material.STONE,
    initialDisplayName: Optional<Component> = Optional.empty(),
    initialLore: List<Component> = emptyList(),
    initialAmount: Int = 1
) : Configurable<ItemData>() {

    val material = edit(
        "material", initialMaterial, MATERIAL,
        icon = { mat -> ItemStack(mat).apply { editMeta { it.displayName(Component.text("Material: ${mat.name}")) } } },
        handler = ChatInputHandler("Enter Material:") { Material.matchMaterial(it) }
    )

    val amount = edit(
        "amount", initialAmount, INT,
        icon = { amt ->
            ItemStack(Material.PAPER).apply {
                amount = amt; editMeta { it.displayName(Component.text("Amount: $amt")) }
            }
        },
        handler = ChatInputHandler.forInteger("Enter Amount:")
    )

    // A list of simple objects (Lore) using the new behavior pattern
    val lore = editList(
        "lore", initialLore, COMPONENT,
        behavior = ElementBehavior(
            icon = { line -> ItemStack(Material.PAPER).apply { editMeta { it.displayName(line) } } },
            handler = ChatInputHandler.forComponent("Enter line:")
        ),
        addButtonClick = { player, accept ->
            player.closeInventory()
            player.sendMessage("Enter line:")
            ChatInput.createHandle(listOf("cancel")).await(player).thenAccept {
                accept(it?.toMMComponent())
            }
        },
        listIcon = { list -> ItemStack(Material.BOOK).apply { editMeta { it.displayName(Component.text("Edit Lore (${list.size} lines)")) } } },
        guiHandler = { player, editor, update ->
            val context = player.getEditorContext() ?: return@editList
            ListGuiHandlerImpl<Component>(context).open(player, editor, update)
        }
    )

    override fun copy(): ItemData {
        return ItemData().also { copy ->
            copy.material.value = this.material.value
            copy.amount.value = this.amount.value
            copy.lore.value = this.lore.value.map { it.clone() }.toMutableList()
        }
    }
}

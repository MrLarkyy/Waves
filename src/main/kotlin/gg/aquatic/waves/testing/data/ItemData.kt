package gg.aquatic.waves.testing.data

import gg.aquatic.stacked.impl.StackedItemImpl
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.edit.editComponentList
import gg.aquatic.waves.editor.edit.editInt
import gg.aquatic.waves.editor.edit.editOptionalComponent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import java.util.*

class ItemData(
    initialMaterial: Material = Material.STONE,
    initialDisplayName: Optional<Component> = Optional.empty(),
    initialLore: List<Component> = emptyList(),
    initialAmount: Int = 1
) : Configurable<ItemData>() {

    val material = editMaterial("material", initialMaterial, "Enter Material Name:")
    val amount = editInt("amount", initialAmount, "Enter Amount (1-64):")

    val displayName = editOptionalComponent("display-name", initialDisplayName,
        "Enter Display Name (type null to remove):"
    )
    val lore = editComponentList("lore", initialLore)

    fun asStacked(): StackedItemImpl {
        return stackedItem(material.value) {
            lore += this@ItemData.lore.value.map { it.value }
            amount = this@ItemData.amount.value
        }
    }
}

package gg.aquatic.waves.testing.data

import gg.aquatic.stacked.impl.StackedItemImpl
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
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

    val lore = editComponentList("lore", initialLore)

    fun asStacked(): StackedItemImpl {
        return stackedItem(material.value) {
            lore += this@ItemData.lore.value.map { it.value }
            amount = this@ItemData.amount.value
        }
    }

    override fun copy(): ItemData {
        return ItemData().also { copy ->
            copy.material.value = this.material.value
            copy.amount.value = this.amount.value
            copy.lore.value = this.lore.value.map { it.clone() }.toMutableList()
        }
    }
}

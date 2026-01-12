package gg.aquatic.waves.util

import org.bukkit.inventory.ItemStack

fun ItemStack.editMeta(block: ItemStack.() -> Unit): ItemStack {
    val meta = itemMeta ?: return this
    block.invoke(this)
    itemMeta = meta
    return this
}
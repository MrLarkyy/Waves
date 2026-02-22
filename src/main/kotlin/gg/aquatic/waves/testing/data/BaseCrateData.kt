package gg.aquatic.waves.testing.data

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class BaseCrateData(
    val id: String,
    val displayName: Component,
    initialItems: List<ItemData> = emptyList(),
    initialRewards: Map<String, ItemData> = emptyMap()
) : Configurable<BaseCrateData>() {

    val items = editConfigurableList(
        key = "items",
        initial = initialItems,
        factory = { ItemData() },
        addButton = { player, accept ->
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
            player.sendMessage("Enter initial material for the item:")

            val material = ChatInput.createHandle(listOf("cancel"))
                .awaitMaterial(player)
            if (material == null) {
                accept(null)
                player.sendMessage("Cancelled.")
                return@editConfigurableList
            }
            accept(ItemData(initialMaterial = material))
        },
        listIcon = { list ->
            ItemStack(Material.CHEST).apply {
                editMeta { it.displayName(Component.text("Crate Items (${list.size})")) }
            }
        },
        itemIcon = { itemData ->
            // Reuse the existing material from ItemData for the icon
            itemData.asStacked().getItem()
        }
    )

    val rewards = editConfigurableMap(
        key = "rewards",
        initial = initialRewards,
        factory = { ItemData() },
        addButton = { player, accept ->
            player.closeInventory()
            player.sendMessage("Enter unique Reward ID:")

            val id = ChatInput.createHandle(listOf("cancel")).await(player)
            if (id == null) {
                accept(null, null)
                return@editConfigurableMap
            }

            player.sendMessage("Enter initial material for '$id':")
            val material = ChatInput.createHandle(listOf("cancel")).awaitMaterial(player)
            if (material != null) {
                accept(id, ItemData(initialMaterial = material))
            } else {
                player.sendMessage("Invalid material! Cancelled.")
                accept(null, null)
            }
        },
        listIcon = { map ->
            ItemStack(Material.CHEST_MINECART).apply {
                editMeta { it.displayName(Component.text("Rewards Map (${map.size} entries)")) }
            }
        },
        itemIcon = { key, itemData ->
            itemData.asStacked().getItem().apply {
                editMeta { it.displayName(Component.text("Reward: $key")) }
            }
        }
    )

    override fun copy(): BaseCrateData {
        return BaseCrateData(
            id,
            displayName,
            items.value.map { it.value.copy() },
            rewards.value.associate { it.key to it.value.copy() }
        )
    }
}

package gg.aquatic.waves.testing.data

import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.input.impl.ChatInput
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
            player.closeInventory()
            player.sendMessage("Enter initial material for the item:")

            ChatInput.createHandle(listOf("cancel"))
                .awaitMaterial(player).thenAccept { material ->
                    if (material == null) {
                        accept(null)
                        player.sendMessage("Cancelled.")
                        return@thenAccept
                    }
                    accept(ItemData(initialMaterial = material))
                }
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

            ChatInput.createHandle(listOf("cancel")).await(player).thenAccept { id ->
                if (id == null) {
                    accept(null, null)
                    return@thenAccept
                }

                player.sendMessage("Enter initial material for '$id':")
                ChatInput.createHandle(listOf("cancel")).awaitMaterial(player).thenAccept { material ->
                    if (material != null) {
                        accept(id, ItemData(initialMaterial = material))
                    } else {
                        player.sendMessage("Invalid material! Cancelled.")
                        accept(null, null)
                    }
                }
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
package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.coroutines.resume

object EditorSelectionMenus {
    suspend fun selectScalarOption(
        player: Player,
        title: String,
        allowed: List<String>,
        current: String? = null,
        nullable: Boolean = false,
    ): String? {
        val entrySlots = (0..44).toList()
        val inventoryType = when {
            allowed.size <= 9 -> InventoryType.GENERIC9X3
            allowed.size <= 18 -> InventoryType.GENERIC9X4
            allowed.size <= 27 -> InventoryType.GENERIC9X5
            else -> InventoryType.GENERIC9X6
        }

        return suspendCancellableCoroutine { continuation ->
            var completed = false

            fun complete(result: String?) {
                if (completed || !continuation.isActive) return
                completed = true
                continuation.resume(result)
            }

            CoroutineScope(continuation.context).launch {
                player.createMenu(Component.text(title.take(32)), inventoryType) {
                    menuFactory = { customTitle, customType, customPlayer, cancelInteractions ->
                        object : PrivateMenu(customTitle, customType, customPlayer, cancelInteractions) {
                            override suspend fun onClosed(player: Player) {
                                complete(current)
                            }
                        }
                    }
                    allowed.take(entrySlots.size).forEachIndexed { index, value ->
                        button("option_$index", entrySlots[index]) {
                            item = stackedItem(
                                if (value == current) Material.LIME_DYE else Material.COMPARATOR
                            ) {
                                displayName = EditorItemStyling.title(value)
                                lore += EditorItemStyling.section("Actions")
                                lore += EditorItemStyling.wrappedActions(
                                    listOf(if (value == current) "Current value" else "Left click to select")
                                )
                            }.getItem()
                            onClick { complete(value) }
                        }
                    }

                    if (nullable) {
                        button("option_clear", 49) {
                            item = stackedItem(Material.BARRIER) {
                                displayName = EditorItemStyling.title("Clear")
                                lore += EditorItemStyling.section("Actions")
                                lore += EditorItemStyling.wrappedActions(listOf("Left click to set this value to null"))
                            }.getItem()
                            onClick { complete(null) }
                        }
                    }

                    button("option_cancel", cancelSlot(inventoryType)) {
                        item = stackedItem(Material.ARROW) {
                            displayName = Component.text("Cancel", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)
                            lore += EditorItemStyling.section("Actions")
                            lore += EditorItemStyling.wrappedActions(listOf("Left click to keep the current value"))
                        }.getItem()
                        onClick { complete(current) }
                    }
                }.open(player)
            }
        }
    }

    private fun cancelSlot(inventoryType: InventoryType): Int = when (inventoryType) {
        InventoryType.GENERIC9X6 -> 53
        InventoryType.GENERIC9X5 -> 44
        InventoryType.GENERIC9X4 -> 35
        else -> 26
    }
}

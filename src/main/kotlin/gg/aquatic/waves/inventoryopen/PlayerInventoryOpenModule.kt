package gg.aquatic.waves.inventoryopen

import gg.aquatic.common.event
import gg.aquatic.kevent.SuspendingEventBus
import gg.aquatic.kevent.suspendingEventBusBuilder
import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.api.event.packet.*
import gg.aquatic.pakket.packetEvent
import gg.aquatic.pakket.sendPacket
import kotlinx.coroutines.CoroutineScope
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This module manages clientside player inventory opening listening and publishing the event for further processing.
 *
 * The `PlayerInventoryOpenModule` observes and manipulates player's inventory and recipe book settings using
 * events and packets. It ensures specific behaviors related to crafting and barrier items in players' inventories.
 */
object PlayerInventoryOpenModule {
    private val settings = ConcurrentHashMap<UUID, Pair<Boolean, Boolean>>()
    private const val recipeId = -1

    lateinit var bus: SuspendingEventBus

    fun initialize(scope: CoroutineScope) {
        bus = suspendingEventBusBuilder { this.scope = scope }
        packetEvent<PacketContainerOpenEvent> {
            val containerId = it.containerId
            if (containerId == 126) return@packetEvent
            removeBarrierRecipe(it.player)
            fallbackCursor(it.player)
        }

        packetEvent<PacketContainerSetSlotEvent> {
            val containerId = it.inventoryId
            if (containerId == 0 && it.slot == 1) {
                it.item = createBarrier()
            }
        }

        packetEvent<PacketContainerContentEvent> {
            if (it.inventoryId == 0) {
                it.contents[1] = createBarrier()
            }
        }

        packetEvent<PacketRecipeBookSeenRecipeReceiveEvent> {
            if (it.recipeId == recipeId) {
                removeBarrierRecipe(it.player)
                fallbackCursor(it.player)

                bus.post(PlayerInventoryOpenEvent(it.player))
            }
        }

        packetEvent<PacketRecipeBookChangeSettingsReceiveEvent> {
            if (it.type == PlayerRecipeBookSettingsChangeEvent.RecipeBookType.CRAFTING) {
                settings[it.player.uniqueId] = Pair(it.isOpen, it.filtering)
            }
        }

        packetEvent<PacketContainerCloseEvent> {
            addBarrierRecipe(it.player)
            barrierCursor(it.player)
        }

        event<PlayerJoinEvent> {
            barrierCursor(it.player)
            addBarrierRecipe(it.player)
        }
    }

    fun addBarrierRecipe(player: Player) {
        val item = createBarrier()
        val recipePacket = Pakket.handler.createRecipeBookAddPacket(
            recipeId,
            ShapelessRecipe(
                NamespacedKey.fromString("aquatic:barrier_recipe")!!,
                item,
            ).apply {
                addIngredient(item)
            },
            false,
            highlight = true,
            replace = true
        )
        player.sendPacket(recipePacket)
    }

    fun removeBarrierRecipe(player: Player) {
        val packet = Pakket.handler.createRecipeBookRemovePacket(mutableListOf(this.recipeId))
        player.sendPacket(packet)
    }

    fun fallbackCursor(player: Player) {
        val stateId = getStateId(player)
        val setSlotPacket = Pakket.handler.createSetSlotItemPacket(0, stateId, 1, ItemStack.of(Material.AIR))
        player.sendPacket(setSlotPacket, true)

        val (open, filtering) = settings[player.uniqueId] ?: Pair(true, false)
        val bookSettingsPacket = Pakket.handler.createRecipeBookSettingsPacket(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.CRAFTING, open,
            filtering = filtering
        )
        player.sendPacket(bookSettingsPacket, false)
    }

    fun barrierCursor(player: Player) {
        val stateId = getStateId(player)
        val item = createBarrier()

        val setSlotPacket = Pakket.handler.createSetSlotItemPacket(0, stateId, 1, item)
        player.sendPacket(setSlotPacket, true)

        val bookSettingsPacket = Pakket.handler.createRecipeBookSettingsPacket(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.CRAFTING, true,
            filtering = true
        )
        player.sendPacket(bookSettingsPacket, false)
    }

    fun getStateId(player: Player): Int {
        return Pakket.handler.getPlayerInventoryState(player)
    }

    fun createBarrier(): ItemStack {
        return ItemStack(Material.STRUCTURE_VOID)
    }
}
package gg.aquatic.waves.interactable

import gg.aquatic.common.event
import gg.aquatic.waves.interactable.type.MEGInteractable
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object InteractableModule {

    /*
    val blockInteractables = mutableListOf<BlockInteractable>()
    val entityInteractables = mutableListOf<EntityInteractable>()
     */
    internal val megInteractables = ConcurrentHashMap.newKeySet<MEGInteractable>()
    private val interactables = ConcurrentHashMap.newKeySet<Interactable>()

    fun register(interactable: Interactable) {
        interactables.add(interactable)
        if (interactable is MEGInteractable) {
            megInteractables.add(interactable)
        }
    }

    fun unregister(interactable: Interactable) {
        interactables.remove(interactable)
        if (interactable is MEGInteractable) {
            megInteractables.remove(interactable)
        }
    }

    fun shutdown() {
        interactables.forEach { it.destroy() }
        interactables.clear()
    }

    fun initialize() {
        if (Bukkit.getPluginManager().getPlugin("ModelEngine") != null) {
            MEGInteractableHandler()
        }

        event<PlayerJoinEvent> { event ->
            for (meg in megInteractables) {
                meg.updateVisibility(event.player)
            }
        }

        event<PlayerQuitEvent> { event ->
            for (meg in megInteractables) {
                // Remove player from internal viewer sets to prevent memory leaks
                meg.removeViewer(event.player)
            }
        }
    }
}
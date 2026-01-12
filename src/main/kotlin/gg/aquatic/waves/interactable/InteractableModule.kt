package gg.aquatic.waves.interactable

import gg.aquatic.common.event
import gg.aquatic.waves.interactable.type.MEGInteractable
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object InteractableModule {

    /*
    val blockInteractables = mutableListOf<BlockInteractable>()
    val entityInteractables = mutableListOf<EntityInteractable>()
     */
    val megInteractables = mutableListOf<MEGInteractable>()

    fun initialize() {
        if (Bukkit.getPluginManager().getPlugin("ModelEngine") != null) {
            MEGInteractableHandler()
        }
        event<PlayerJoinEvent> { e ->
            for (tickableObject in megInteractables) {
                if (tickableObject.audience.canBeApplied(e.player)) {
                    tickableObject.addViewer(e.player)
                }
            }
        }

        event<PlayerQuitEvent> {
            for (tickableObject in megInteractables) {
                tickableObject.removeViewer(it.player)
            }
        }
    }
}
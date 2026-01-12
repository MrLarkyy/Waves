package gg.aquatic.waves.interactable

import gg.aquatic.waves.audience.AquaticAudience
import org.bukkit.Location
import org.bukkit.entity.Player

abstract class Interactable {

    abstract val onInteract: (InteractableInteractEvent) -> Unit

    abstract val location: Location
    abstract var audience: AquaticAudience
    abstract val viewers: Collection<Player>

    abstract fun destroy()

    fun trigger(player: Player, isLeftClick: Boolean) {
        onInteract(InteractableInteractEvent(this, player, isLeftClick))
    }
}
package gg.aquatic.waves.interactable

import gg.aquatic.waves.audience.AquaticAudience
import org.bukkit.Location
import org.bukkit.entity.Player

abstract class Interactable {

    abstract val onInteract: (InteractableInteractEvent) -> Unit

    abstract var audience: AquaticAudience
    abstract val location: Location
    abstract val viewers: Collection<Player>

    abstract fun addViewer(player: Player)
    abstract fun removeViewer(player: Player)

    abstract fun destroy()

    abstract fun updateViewers()

}
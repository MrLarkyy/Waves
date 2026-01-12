package gg.aquatic.waves.interactable.type

import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.entity.FakeEntity
import gg.aquatic.waves.interactable.Interactable
import gg.aquatic.waves.interactable.InteractableInteractEvent
import org.bukkit.Location
import org.bukkit.entity.Player

class EntityInteractable(
    val entity: FakeEntity,
    override val onInteract: (InteractableInteractEvent) -> Unit
) : Interactable() {

    override var audience: AquaticAudience
        get() = entity.audience
        set(value) { entity.setAudience(value) }

    override val location: Location get() = entity.location
    override val viewers: Collection<Player> get() = entity.viewers

    init {
        entity.onInteract = { e ->
            this.trigger(e.player, e.isLeftClick)
        }
        if (!entity.registered) entity.register()
    }

    override fun destroy() {
        entity.destroy()
    }
}
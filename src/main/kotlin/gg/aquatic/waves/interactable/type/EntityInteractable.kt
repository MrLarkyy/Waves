package gg.aquatic.waves.interactable.type

import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.entity.FakeEntity
import gg.aquatic.waves.interactable.Interactable
import gg.aquatic.waves.interactable.InteractableInteractEvent
import org.bukkit.Location
import org.bukkit.entity.Player

class EntityInteractable(val entity: FakeEntity, override val onInteract: (InteractableInteractEvent) -> Unit) : Interactable() {

    override var audience: AquaticAudience
        get() {
            return entity.audience
        }
        set(value) {
            entity.setAudience(value)
        }

    override val location: Location
        get() {
            return entity.location
        }
    override val viewers: Collection<Player>
        get() {
            return entity.viewers()
        }

    init {
        entity.onInteract = { e ->
            this.onInteract(
                InteractableInteractEvent(
                    this,
                    e.player,
                    e.isLeftClick
                )
            )
        }
    }

    override fun addViewer(player: Player) {
        entity.addViewer(player)
    }

    override fun removeViewer(player: Player) {
        entity.removeViewer(player)
    }

    override fun destroy() {
        this.entity.destroy()
    }

    override fun updateViewers() {
        entity.tickRange(true)
    }
}
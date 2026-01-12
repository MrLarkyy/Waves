package gg.aquatic.waves.interactable.type

import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.block.FakeBlock
import gg.aquatic.waves.interactable.Interactable
import gg.aquatic.waves.interactable.InteractableInteractEvent
import org.bukkit.Location
import org.bukkit.entity.Player

class BlockInteractable(
    val block: FakeBlock,
    override val onInteract: (InteractableInteractEvent) -> Unit
) : Interactable() {

    override var audience: AquaticAudience
        get() = block.audience
        set(value) { block.setAudience(value) }

    override val location: Location get() = block.location
    override val viewers: Collection<Player> get() = block.viewers

    init {
        // Professional Delegation: Tell the block to trigger this interactable
        block.onInteract = { e ->
            this.trigger(e.player, e.isLeftClick)
        }
        // Ensure the block is active in the world
        if (!block.registered) block.register()
    }

    override fun destroy() {
        block.destroy()
    }
}
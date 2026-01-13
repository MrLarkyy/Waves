package gg.aquatic.waves.interactable.type

import gg.aquatic.blokk.MultiBlokk
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.block.FakeBlock
import gg.aquatic.waves.interactable.Interactable
import gg.aquatic.waves.interactable.InteractableInteractEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class MultiBlockInteractable(
    val multiBlokk: MultiBlokk,
    override val location: Location,
    val viewRange: Int,
    initialAudience: AquaticAudience,
    override val onInteract: (InteractableInteractEvent) -> Unit,
    onTick: suspend () -> Unit = {}
) : Interactable() {

    private val blocks = ConcurrentHashMap.newKeySet<FakeBlock>()

    override var audience: AquaticAudience = initialAudience
        set(value) {
            field = value
            blocks.forEach { it.setAudience(value) }
        }

    override val viewers: Collection<Player>
        get() = blocks.firstOrNull()?.viewers ?: emptyList()

    init {
        // Use the MultiBlokk logic to spawn the fake blocks
        multiBlokk.processLayerCells(location) { char, newLoc ->
            val blokk = multiBlokk.shape.blocks[char] ?: return@processLayerCells

            val fakeBlock = FakeBlock(blokk, newLoc, viewRange, audience, onTick = onTick)

            // Delegate interactions to the MultiBlock container
            fakeBlock.onInteract = { e ->
                this.trigger(e.player, e.isLeftClick)
            }

            fakeBlock.register()
            blocks.add(fakeBlock)
        }
    }

    override fun destroy() {
        blocks.forEach { it.destroy() }
        blocks.clear()
    }
}
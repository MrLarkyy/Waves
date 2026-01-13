package gg.aquatic.waves.clientside.block

import gg.aquatic.blokk.Blokk
import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.sendPacket
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.FakeObject
import gg.aquatic.waves.clientside.FakeObjectHandler
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class FakeBlock(
    block: Blokk,
    location: Location,
    override val viewRange: Int,
    audience: AquaticAudience,
    var onInteract: (FakeBlockInteractEvent) -> Unit = {},
    var onTick: suspend () -> Unit = {}
) : FakeObject(viewRange, audience) {

    override val location: Location = location.toBlockLocation().apply { yaw = location.yaw }
    var block: Blokk = block
        private set

    init {
        setAudience(audience)
    }

    fun changeBlock(aquaticBlock: Blokk) {
        this.block = aquaticBlock
        isViewing.forEach { onShow(it) }
    }

    override fun onShow(player: Player) {
        val packet = Pakket.handler.createBlockChangePacket(location, block.blockData)
        player.sendPacket(packet, true)
    }

    override fun onHide(player: Player) {
        val packet = Pakket.handler.createBlockChangePacket(location, location.block.blockData)
        player.sendPacket(packet, false)
    }


    override fun handleInteract(player: Player, isLeftClick: Boolean) {
        val event = FakeBlockInteractEvent(this, player, isLeftClick)
        onInteract.invoke(event)

        // Anti-ghosting correction for right-clicks
        if (!isLeftClick && !destroyed) {
            onShow(player)
        }
    }

    fun register() {
        if (registered) return
        registered = true

        FakeObjectHandler.locationToBlocks.getOrPut(this.location) { ConcurrentHashMap.newKeySet() } += this
        FakeObjectHandler.tickableObjects += this
        val chunk = location.chunk
        val bundle = FakeObjectHandler.getOrCreateChunkCacheBundle(chunk.x, chunk.z, chunk.world)
        bundle.blocks += this
    }

    fun unregister() {
        if (!registered) return
        registered = false
        val chunk = location.chunk
        val bundle = FakeObjectHandler.getChunkCacheBundle(chunk.x, chunk.z, chunk.world) ?: return
        bundle.blocks -= this
    }

    override fun destroy() {
        destroyed = true
        isViewing.forEach { hide(it) }
        FakeObjectHandler.tickableObjects -= this
        unregister()
        FakeObjectHandler.locationToBlocks[location]?.remove(this)
    }

    override suspend fun tick() {
        onTick()
    }
}
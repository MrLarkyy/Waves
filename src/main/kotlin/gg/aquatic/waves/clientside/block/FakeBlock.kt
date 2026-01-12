package gg.aquatic.waves.clientside.block

import gg.aquatic.blokk.Blokk
import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.sendPacket
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.FakeObject
import gg.aquatic.waves.clientside.FakeObjectHandler
import gg.aquatic.waves.util.chunk.trackedBy
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class FakeBlock(
    block: Blokk, location: Location,
    override val viewRange: Int,
    audience: AquaticAudience,
    var onInteract: (FakeBlockInteractEvent) -> Unit = {}
) : FakeObject() {
    override val location: Location = location.toBlockLocation()

    private var _audience = audience

    override val audience: AquaticAudience
        get() = _audience

    override fun setAudience(audience: AquaticAudience) {
        _audience = audience
        for (viewer in viewers()) {
            if (audience.canBeApplied(viewer) && viewer.isOnline) continue
            removeViewer(viewer)
        }
        val viewers = viewers()

        for (player in
        Bukkit.getOnlinePlayers().filter { !viewers.contains(it) }) {
            if (!audience.canBeApplied(player)) continue
            addViewer(player)
        }
    }

    override fun destroy() {
        destroyed = true
        for (player in isViewing()) {
            hide(player)
        }
        FakeObjectHandler.tickableObjects -= this
        unregister()
        FakeObjectHandler.locationToBlocks[location.toBlockLocation()]?.remove(this)
    }

    var block = block
        private set

    init {
        FakeObjectHandler.locationToBlocks.getOrPut(location.toBlockLocation()) { ConcurrentHashMap.newKeySet() } += this
        FakeObjectHandler.tickableObjects += this

        setAudience(audience)

        val chunkViewers = location.chunk.trackedBy().toSet()
        for (viewer in viewers()) {
            if (viewer in chunkViewers) {
                show(viewer)
            }
        }
    }

    fun register() {
        if (registered) return
        registered = true
        val chunk = location.chunk
        val bundle = FakeObjectHandler.getOrCreateChunkCacheBundle(
            chunk.x, chunk.z, chunk.world
        )
        bundle.blocks += this
    }

    fun unregister() {
        if (!registered) return
        registered = false
        val chunk = location.chunk
        val bundle = FakeObjectHandler.getChunkCacheBundle(
            chunk.x, chunk.z, chunk.world
        ) ?: return
        bundle.blocks -= this
    }

    fun changeBlock(aquaticBlock: Blokk) {
        block = aquaticBlock
        for (player in isViewing()) {
            show(player)
        }
    }

    override fun addViewer(player: Player) {
        if (viewers().contains(player)) return
        internalAddViewer(player)
        if (player.world.name != location.world!!.name) return
        if (player.location.distanceSquared(location) <= viewRange * viewRange) {
            show(player)
        }
    }

    override fun removeViewer(uuid: UUID) {
        internalRemoveViewer(uuid)
    }

    override fun removeViewer(player: Player) {
        if (isViewing().contains(player)) {
            hide(player)
        }
        internalRemoveViewer(player)
    }

    override fun show(player: Player) {
        setIsViewing(true, player)
        val packet = Pakket.handler.createBlockChangePacket(location, block.blockData)
        player.sendPacket(packet, true)
    }

    override fun hide(player: Player) {
        setIsViewing(false, player)
        val packet = Pakket.handler.createBlockChangePacket(location, location.block.blockData)
        player.sendPacket(packet, false)
    }

    override suspend fun tick() {

    }
}
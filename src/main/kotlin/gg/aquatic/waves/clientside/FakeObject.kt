package gg.aquatic.waves.clientside

import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.util.chunk.trackedBy
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

abstract class FakeObject {

    abstract val location: Location

    @Volatile
    protected var registered: Boolean = false
    abstract val viewRange: Int

    @Volatile
    var destroyed: Boolean = false
    abstract val audience: AquaticAudience
    abstract fun setAudience(audience: AquaticAudience)

    // List of players that can see the object
    private val viewers = HashSet<Player>()

    // List of players that are currently viewing the object
    private val isViewing = HashSet<Player>()

    abstract fun destroy()
    abstract fun addViewer(player: Player)
    abstract fun removeViewer(uuid: UUID)
    abstract fun removeViewer(player: Player)
    abstract fun show(player: Player)
    abstract fun hide(player: Player)

    abstract suspend fun tick()

    fun viewers(): Collection<Player> {
        return synchronized(viewers) {
            viewers.toList()
        }
    }

    protected fun internalAddViewer(player: Player) {
        synchronized(viewers) {
            viewers.add(player)
        }
    }

    protected fun internalRemoveViewer(player: Player) {
        synchronized(viewers) {
            viewers.remove(player)
        }
    }

    protected fun internalRemoveViewer(uuid: UUID) {
        synchronized(viewers) {
            viewers.removeIf { it.uniqueId == uuid }
        }
    }

    fun isViewing(): Collection<Player> {
        return synchronized(isViewing) {
            isViewing.toList()
        }
    }

    protected fun setIsViewing(isViewing: Boolean, player: Player) {
        synchronized(this.isViewing) {
            if (isViewing) {
                this.isViewing.add(player)
            } else {
                this.isViewing.remove(player)
            }
        }
    }

    protected fun removeIsViewing(uuid: UUID) {
        synchronized(this.isViewing) {
            isViewing.removeIf { it.uniqueId == uuid }
        }
    }

    private var rangeTick = 0
    internal suspend fun handleTick() {
        tick()
        tickRange()
    }

    internal fun tickRange(forced: Boolean = false) {
        if (!forced) {
            rangeTick++
            if (rangeTick % 4 == 0) {
                rangeTick = 0
            } else {
                return
            }
        }
        synchronized(viewers) {
            val trackedPlayers = location.chunk.trackedBy()
            val loadedChunkViewers = trackedPlayers.filter { viewers.contains(it) }
            for (loadedChunkViewer in loadedChunkViewers.toSet()) {
                if (!loadedChunkViewer.isOnline) {
                    FakeObjectHandler.handlePlayerRemove(loadedChunkViewer, this@FakeObject, true)
                    continue
                }
                if (loadedChunkViewer.world != location.world) {
                    FakeObjectHandler.handlePlayerRemove(loadedChunkViewer, this@FakeObject)
                    continue
                }
                val distance = loadedChunkViewer.location.distanceSquared(location)
                synchronized(isViewing) {
                    if (isViewing.contains(loadedChunkViewer)) {
                        if (distance > viewRange * viewRange) {
                            hide(loadedChunkViewer)
                            isViewing.remove(loadedChunkViewer)
                        }
                    } else {
                        if (distance <= viewRange * viewRange) {
                            show(loadedChunkViewer)
                        }
                    }
                }
            }
        }
    }
}
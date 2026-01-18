package gg.aquatic.waves.hologram

import gg.aquatic.common.event
import gg.aquatic.common.ticker.Ticker
import gg.aquatic.snapshotmap.SnapshotMap
import gg.aquatic.waves.util.chunk.ChunkId
import gg.aquatic.waves.util.chunk.chunkId
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

object HologramHandler {
    val tickingHolograms = SnapshotMap<ChunkId, MutableCollection<Hologram>>()
    val waitingHolograms = SnapshotMap<ChunkId, MutableCollection<Hologram>>()

    fun onLoad() {
        Ticker {
            tickingHolograms.forEach { (_, list) ->
                val iterator = list.iterator()
                while (iterator.hasNext()) {
                    val hologram = iterator.next()
                    try {
                        hologram.tick()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.register()

        event<ChunkLoadEvent> {
            val chunkId = it.chunk.chunkId()
            val toLoad = waitingHolograms.remove(chunkId) ?: return@event
            val list = tickingHolograms.getOrPut(chunkId) { ArrayList() }
            for (hologram in toLoad) {
                hologram.chunk = it.chunk
                list += hologram
            }
        }
        event<ChunkUnloadEvent> {
            val chunkId = it.chunk.chunkId()
            val toWait = tickingHolograms.remove(chunkId) ?: return@event
            val list = waitingHolograms.getOrPut(chunkId) { ArrayList() }
            for (hologram in toWait) {
                hologram.chunk = null
                list += hologram
            }
        }
    }

    fun allHolograms(): Collection<Hologram> {
        return listOf(tickingHolograms.values.flatten(), waitingHolograms.values.flatten()).flatten()
    }

    fun destroyHolograms() {
        for (hologram in allHolograms()) {
            hologram.destroy()
        }
        waitingHolograms.clear()
        tickingHolograms.clear()
    }

    fun removeHologram(hologram: Hologram) {
        for ((_, holograms) in tickingHolograms) {
            holograms.remove(hologram)
        }
        for ((_, holograms) in waitingHolograms) {
            holograms.remove(hologram)
        }
    }
}
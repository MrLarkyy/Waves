package gg.aquatic.waves.hologram

import gg.aquatic.common.event
import gg.aquatic.common.ticker.Ticker
import gg.aquatic.waves.util.chunk.ChunkId
import gg.aquatic.waves.util.chunk.chunkId
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import java.util.concurrent.ConcurrentHashMap

object HologramHandler {
    val tickingHolograms = ConcurrentHashMap<ChunkId, MutableCollection<Hologram>>()
    val waitingHolograms = ConcurrentHashMap<ChunkId, MutableCollection<Hologram>>()

    fun onLoad() {
        Ticker {
            for ((_, holograms) in tickingHolograms) {
                for (hologram in holograms) {
                    hologram.tick()
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
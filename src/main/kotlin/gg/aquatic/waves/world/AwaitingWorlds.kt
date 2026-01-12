package gg.aquatic.waves.world

import gg.aquatic.common.event
import org.bukkit.event.world.WorldLoadEvent

object AwaitingWorlds {

    val awaiting = HashMap<String, AwaitingWorld>()

    fun initialize() {
        event<WorldLoadEvent> {
            awaiting[it.world.name]?.thens?.forEach { then -> then(it.world) }
        }
    }
}
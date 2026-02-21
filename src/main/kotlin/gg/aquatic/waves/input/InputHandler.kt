package gg.aquatic.waves.input

import gg.aquatic.common.event
import gg.aquatic.kregistry.bootstrap.BootstrapHolder
import gg.aquatic.waves.Waves
import gg.aquatic.waves.input.InputHandler.forceCancel
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent

object InputHandler {

    fun disable() {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            forceCancel(onlinePlayer)
        }
    }

    fun forceCancel(player: Player) {
        for (value in Input.REGISTRY.all().values) {
            value.forceCancel(player)
        }
    }
}

fun BootstrapHolder.initializeInput(inputTypes: Map<String, Input>) {
    event<PlayerQuitEvent> {
        forceCancel(it.player)
    }

    Waves.registryBootstrap(this) {
        registry(Input.REGISTRY_KEY) {
            inputTypes.forEach { (key, value) -> add(key, value) }
        }
    }
}
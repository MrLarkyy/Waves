package gg.aquatic.waves.input

import gg.aquatic.common.event
import gg.aquatic.kregistry.MutableRegistry
import gg.aquatic.kregistry.Registry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent

object InputHandler {

    fun initialize(inputTypes: Map<String, Input>) {
        event<PlayerQuitEvent> {
            forceCancel(it.player)
        }

        val registry = MutableRegistry<String, Input>()
        inputTypes.forEach { (key, value) -> registry.register(key, value) }

        Registry.update { registerRegistry(Input.REGISTRY_KEY, registry.freeze()) }
    }

    fun disable() {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            forceCancel(onlinePlayer)
        }
    }

    fun forceCancel(player: Player) {
        for (value in Input.REGISTRY.getAll().values) {
            value.forceCancel(player)
        }
    }
}
package gg.aquatic.waves.input

import gg.aquatic.waves.event
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent

object InputHandler {

    fun initialize() {
        event<PlayerQuitEvent> {
            forceCancel(it.player)
        }
    }

    fun disable() {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            forceCancel(onlinePlayer)
        }
    }

    fun forceCancel(player: Player) {
        for (value in Input.Companion.REGISTRY.getAll().values) {
            value.forceCancel(player)
        }
    }
}
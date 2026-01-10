package gg.aquatic.waves.editor

import gg.aquatic.waves.event
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent

object EditorHandler {

    val contexts = HashMap<Player, EditorContext>()

    fun initialize() {
        event<PlayerQuitEvent> {
            contexts.remove(it.player)
        }
    }

    fun Player.getEditorContext() = contexts[this]
}
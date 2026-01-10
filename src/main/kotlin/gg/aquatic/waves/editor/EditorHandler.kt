package gg.aquatic.waves.editor

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.waves.editor.ui.EditorMenuProvider
import gg.aquatic.waves.event
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent

object EditorHandler {

    val contexts = HashMap<Player, EditorContext>()

    fun initialize() {
        event<PlayerQuitEvent> {
            contexts.remove(it.player)
        }
    }

    fun <T : Configurable<T>> startEditing(
        player: Player,
        title: Component,
        configurable: T,
        onSave: (T) -> Unit
    ) {
        val context = EditorContext(player)
        contexts[player] = context

        // Work on a copy so 'cancel' logic is just closing the menu
        val workingCopy = configurable.copy()

        KMenuCtx.launch {
            EditorMenuProvider.openValueEditor(
                context = context,
                title = title,
                values = workingCopy.getEditorValues(),
                onSave = {
                    onSave(workingCopy)
                }
            )
        }
    }

    fun Player.getEditorContext() = contexts[this]
}
package gg.aquatic.waves.editor

import gg.aquatic.common.event
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.waves.editor.ui.EditorMenuProvider
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

        val workingCopy = configurable.copy()

        KMenuCtx.launch {
            // Use the new navigate system to set the root
            context.navigate {
                EditorMenuProvider.openValueEditor(
                    context = context,
                    title = title,
                    values = workingCopy.getEditorValues(),
                    onSave = { onSave(workingCopy) }
                )
            }
        }
    }

    fun Player.getEditorContext() = contexts[this]
}
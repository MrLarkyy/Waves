package gg.aquatic.waves.editor

import gg.aquatic.common.event
import gg.aquatic.kmenu.KMenu
import gg.aquatic.waves.editor.ui.EditorMenuProvider
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object EditorHandler {

    val contexts = ConcurrentHashMap<Player, EditorContext>()

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

        KMenu.scope.launch {
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

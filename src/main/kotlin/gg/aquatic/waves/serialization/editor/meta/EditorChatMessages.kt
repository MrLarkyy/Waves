package gg.aquatic.waves.serialization.editor.meta

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

object EditorChatMessages {

    fun sendPrompt(
        player: Player,
        prompt: String,
        allowNull: Boolean = false,
        extraHints: List<String> = emptyList(),
    ) {
        player.sendMessage(line(prompt, NamedTextColor.AQUA))
        player.sendMessage(line("Type 'cancel' to go back.", NamedTextColor.GRAY))
        if (allowNull) {
            player.sendMessage(line("Type 'null' to clear this value.", NamedTextColor.GRAY))
        }
        extraHints.forEach { hint ->
            player.sendMessage(line(hint, NamedTextColor.DARK_GRAY))
        }
    }

    fun sendError(player: Player, message: String) {
        player.sendMessage(line(message, NamedTextColor.RED))
    }

    private fun line(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}

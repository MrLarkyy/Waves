package gg.aquatic.waves.editor.handlers

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.input.impl.ChatInput
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

/**
 * A handler that prompts the player in chat.
 * T is the type being edited (e.g., String or Component)
 */
class ChatInputHandler<T>(
    private val prompt: String,
    private val parser: (String) -> T?
) : EditorClickHandler<T> {

    override fun handle(
        player: Player,
        current: T,
        clickType: ButtonType,
        update: (T?) -> Unit,
    ) {
        player.closeInventory()
        player.sendMessage(prompt)

        ChatInput.createHandle(listOf("cancel")).await(player).thenAccept {
            if (it != null) {
                update(parser(it))
            }
            update(null)
        }
    }

    companion object {
        // Shorthand for simple Strings
        fun forString(prompt: String) = ChatInputHandler(prompt) { it }

        fun forOptionalString(prompt: String) = ChatInputHandler(prompt) { str ->
            if (str.lowercase() == "null") Optional.empty()
            else Optional.of(str)
        }

        // Shorthand for Adventure Components
        fun forComponent(prompt: String) = ChatInputHandler(prompt) {
            MiniMessage.miniMessage().deserialize(it)
        }

        fun forOptionalComponent(prompt: String) = ChatInputHandler(prompt) { str ->
            if (str.lowercase() == "null") Optional.empty()
            else Optional.of(MiniMessage.miniMessage().deserialize(str))
        }

        fun forInteger(prompt: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) =
            ChatInputHandler(prompt) {
                val i = it.toIntOrNull()
                i?.coerceIn(min, max)
            }

        fun forMaterial(prompt: String) = ChatInputHandler(prompt) { Material.matchMaterial(it) }
    }
}

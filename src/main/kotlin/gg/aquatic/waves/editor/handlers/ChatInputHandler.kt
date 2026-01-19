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

        fun forDouble(prompt: String, min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE) =
            ChatInputHandler(prompt) {
                val d = it.toDoubleOrNull()
                d?.coerceIn(min, max)
            }

        fun forBoolean(prompt: String) = ChatInputHandler(prompt) { it.toBooleanStrictOrNull() }

        fun forMaterial(prompt: String) = forEnum<Material>(prompt)

        inline fun <reified T : Enum<T>> forEnum(prompt: String) = ChatInputHandler(prompt) { str ->
            try {
                java.lang.Enum.valueOf(T::class.java, str.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun forSound(prompt: String) = ChatInputHandler(prompt) { key ->
            try {
                org.bukkit.Registry.SOUNDS.getOrThrow(org.bukkit.NamespacedKey.minecraft(key.lowercase()))
            } catch (e: Exception) {
                null
            }
        }
    }
}

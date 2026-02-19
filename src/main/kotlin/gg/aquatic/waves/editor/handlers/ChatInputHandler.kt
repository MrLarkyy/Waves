package gg.aquatic.waves.editor.handlers

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.quickminimessage.MMParser
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.input.impl.ChatInput
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

    override suspend fun handle(
        player: Player,
        current: T,
        clickType: ButtonType,
        update: suspend (T?) -> Unit,
    ) {
        player.closeInventory()
        player.sendMessage(prompt)

        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        if (input != null) {
            update(parser(input))
        } else {
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
            MMParser.deserialize(it)
        }

        fun forOptionalComponent(prompt: String) = ChatInputHandler(prompt) { str ->
            if (str.lowercase() == "null") Optional.empty()
            else Optional.of(MMParser.deserialize(str))
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

        fun forFloat(prompt: String, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE) =
            ChatInputHandler(prompt) {
                val f = it.toFloatOrNull()
                f?.coerceIn(min, max)
            }

        fun forLong(prompt: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE) =
            ChatInputHandler(prompt) {
                val l = it.toLongOrNull()
                l?.coerceIn(min, max)
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

package gg.aquatic.waves.editor.handlers

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.quickminimessage.MMParser
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.BigInteger
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
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }

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

        fun forInteger(prompt: String, min: Int? = null, max: Int? = null) =
            ChatInputHandler(prompt) {
                val i = it.toIntOrNull()
                i?.coerceIn(min, max)
            }

        fun forDouble(prompt: String, min: Double? = null, max: Double? = null) =
            ChatInputHandler(prompt) {
                val d = it.toDoubleOrNull()
                d?.coerceIn(min, max)
            }

        fun forFloat(prompt: String, min: Float? = null, max: Float? = null) =
            ChatInputHandler(prompt) {
                val f = it.toFloatOrNull()
                f?.coerceIn(min, max)
            }

        fun forLong(prompt: String, min: Long? = null, max: Long? = null) =
            ChatInputHandler(prompt) {
                val l = it.toLongOrNull()
                l?.coerceIn(min, max)
            }

        fun forShort(prompt: String, min: Short? = null, max: Short? = null) =
            ChatInputHandler(prompt) {
                val l = it.toShortOrNull()
                l?.coerceIn(min, max)
            }

        fun forBigInt(prompt: String, min: BigInteger? = null, max: BigInteger? = null) =
            ChatInputHandler(prompt) {
                val l = it.toBigIntegerOrNull()
                l?.coerceIn(min, max)
            }

        fun forBigDecimal(prompt: String, min: BigDecimal? = null, max: BigDecimal? = null) =
            ChatInputHandler(prompt) {
                val l = it.toBigDecimalOrNull()
                l?.coerceIn(min, max)
            }

        fun forByte(prompt: String, min: Byte? = null, max: Byte? = null) =
            ChatInputHandler(prompt) {
                val b = it.toByteOrNull()
                b?.coerceIn(min, max)
            }

        fun forUInt(prompt: String, min: UInt? = null, max: UInt? = null) =
            ChatInputHandler(prompt) {
                it.toUIntOrNull()?.coerceIn(min, max)
            }

        fun forULong(prompt: String, min: ULong? = null, max: ULong? = null) =
            ChatInputHandler(prompt) {
                it.toULongOrNull()?.coerceIn(min, max)
            }

        fun forUShort(prompt: String, min: UShort? = null, max: UShort? = null) =
            ChatInputHandler(prompt) {
                it.toUShortOrNull()?.coerceIn(min, max)
            }

        fun forUByte(prompt: String, min: UByte? = null, max: UByte? = null) =
            ChatInputHandler(prompt) {
                it.toUByteOrNull()?.coerceIn(min, max)
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

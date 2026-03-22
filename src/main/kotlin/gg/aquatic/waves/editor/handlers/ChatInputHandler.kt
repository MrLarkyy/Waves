package gg.aquatic.waves.editor.handlers

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.quickminimessage.MMParser
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
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

        fun forStringList(prompt: String) = ChatInputHandler(prompt) { raw ->
            parseList(raw) { it }
        }

        fun forOptionalStringList(prompt: String) = ChatInputHandler(prompt) { raw ->
            val input = raw.trim()
            if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                Optional.empty<List<String>>()
            } else {
                val parsed = parseList(input) { it }
                if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
            }
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

        fun forIntegerList(prompt: String, min: Int? = null, max: Int? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toIntOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalIntegerList(prompt: String, min: Int? = null, max: Int? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Int>>()
                } else {
                    val parsed = parseList(input) { it.toIntOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forOptionalInteger(prompt: String, min: Int? = null, max: Int? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Int>()
                } else {
                    input.toIntOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forDouble(prompt: String, min: Double? = null, max: Double? = null) =
            ChatInputHandler(prompt) {
                val d = it.toDoubleOrNull()
                d?.coerceIn(min, max)
            }

        fun forDoubleList(prompt: String, min: Double? = null, max: Double? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toDoubleOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalDoubleList(prompt: String, min: Double? = null, max: Double? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Double>>()
                } else {
                    val parsed = parseList(input) { it.toDoubleOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forOptionalDouble(prompt: String, min: Double? = null, max: Double? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Double>()
                } else {
                    input.toDoubleOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forFloat(prompt: String, min: Float? = null, max: Float? = null) =
            ChatInputHandler(prompt) {
                val f = it.toFloatOrNull()
                f?.coerceIn(min, max)
            }

        fun forFloatList(prompt: String, min: Float? = null, max: Float? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toFloatOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalFloatList(prompt: String, min: Float? = null, max: Float? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Float>>()
                } else {
                    val parsed = parseList(input) { it.toFloatOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forOptionalFloat(prompt: String, min: Float? = null, max: Float? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Float>()
                } else {
                    input.toFloatOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forLong(prompt: String, min: Long? = null, max: Long? = null) =
            ChatInputHandler(prompt) {
                val l = it.toLongOrNull()
                l?.coerceIn(min, max)
            }

        fun forLongList(prompt: String, min: Long? = null, max: Long? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toLongOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalLongList(prompt: String, min: Long? = null, max: Long? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Long>>()
                } else {
                    val parsed = parseList(input) { it.toLongOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forOptionalLong(prompt: String, min: Long? = null, max: Long? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Long>()
                } else {
                    input.toLongOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forShort(prompt: String, min: Short? = null, max: Short? = null) =
            ChatInputHandler(prompt) {
                val l = it.toShortOrNull()
                l?.coerceIn(min, max)
            }

        fun forShortList(prompt: String, min: Short? = null, max: Short? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toShortOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalShort(prompt: String, min: Short? = null, max: Short? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Short>()
                } else {
                    input.toShortOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalShortList(prompt: String, min: Short? = null, max: Short? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Short>>()
                } else {
                    val parsed = parseList(input) { it.toShortOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forBigInt(prompt: String, min: BigInteger? = null, max: BigInteger? = null) =
            ChatInputHandler(prompt) {
                val l = it.toBigIntegerOrNull()
                l?.coerceIn(min, max)
            }

        fun forBigIntList(prompt: String, min: BigInteger? = null, max: BigInteger? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toBigIntegerOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalBigInt(prompt: String, min: BigInteger? = null, max: BigInteger? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<BigInteger>()
                } else {
                    input.toBigIntegerOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalBigIntList(prompt: String, min: BigInteger? = null, max: BigInteger? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<BigInteger>>()
                } else {
                    val parsed = parseList(input) { it.toBigIntegerOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forBigDecimal(prompt: String, min: BigDecimal? = null, max: BigDecimal? = null) =
            ChatInputHandler(prompt) {
                val l = it.toBigDecimalOrNull()
                l?.coerceIn(min, max)
            }

        fun forBigDecimalList(prompt: String, min: BigDecimal? = null, max: BigDecimal? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toBigDecimalOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalBigDecimal(prompt: String, min: BigDecimal? = null, max: BigDecimal? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<BigDecimal>()
                } else {
                    input.toBigDecimalOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalBigDecimalList(prompt: String, min: BigDecimal? = null, max: BigDecimal? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<BigDecimal>>()
                } else {
                    val parsed = parseList(input) { it.toBigDecimalOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forByte(prompt: String, min: Byte? = null, max: Byte? = null) =
            ChatInputHandler(prompt) {
                val b = it.toByteOrNull()
                b?.coerceIn(min, max)
            }

        fun forByteList(prompt: String, min: Byte? = null, max: Byte? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toByteOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalByte(prompt: String, min: Byte? = null, max: Byte? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<Byte>()
                } else {
                    input.toByteOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalByteList(prompt: String, min: Byte? = null, max: Byte? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<Byte>>()
                } else {
                    val parsed = parseList(input) { it.toByteOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forUInt(prompt: String, min: UInt? = null, max: UInt? = null) =
            ChatInputHandler(prompt) {
                it.toUIntOrNull()?.coerceIn(min, max)
            }

        fun forUIntList(prompt: String, min: UInt? = null, max: UInt? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toUIntOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalUInt(prompt: String, min: UInt? = null, max: UInt? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<UInt>()
                } else {
                    input.toUIntOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalUIntList(prompt: String, min: UInt? = null, max: UInt? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<UInt>>()
                } else {
                    val parsed = parseList(input) { it.toUIntOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forULong(prompt: String, min: ULong? = null, max: ULong? = null) =
            ChatInputHandler(prompt) {
                it.toULongOrNull()?.coerceIn(min, max)
            }

        fun forULongList(prompt: String, min: ULong? = null, max: ULong? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toULongOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalULong(prompt: String, min: ULong? = null, max: ULong? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<ULong>()
                } else {
                    input.toULongOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalULongList(prompt: String, min: ULong? = null, max: ULong? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<ULong>>()
                } else {
                    val parsed = parseList(input) { it.toULongOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forUShort(prompt: String, min: UShort? = null, max: UShort? = null) =
            ChatInputHandler(prompt) {
                it.toUShortOrNull()?.coerceIn(min, max)
            }

        fun forUShortList(prompt: String, min: UShort? = null, max: UShort? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toUShortOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalUShort(prompt: String, min: UShort? = null, max: UShort? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<UShort>()
                } else {
                    input.toUShortOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalUShortList(prompt: String, min: UShort? = null, max: UShort? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<UShort>>()
                } else {
                    val parsed = parseList(input) { it.toUShortOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forUByte(prompt: String, min: UByte? = null, max: UByte? = null) =
            ChatInputHandler(prompt) {
                it.toUByteOrNull()?.coerceIn(min, max)
            }

        fun forUByteList(prompt: String, min: UByte? = null, max: UByte? = null) =
            ChatInputHandler(prompt) { raw ->
                parseList(raw) { it.toUByteOrNull()?.coerceIn(min, max) }
            }

        fun forOptionalUByte(prompt: String, min: UByte? = null, max: UByte? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<UByte>()
                } else {
                    input.toUByteOrNull()?.coerceIn(min, max)?.let { Optional.of(it) }
                }
            }

        fun forOptionalUByteList(prompt: String, min: UByte? = null, max: UByte? = null) =
            ChatInputHandler(prompt) { raw ->
                val input = raw.trim()
                if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                    Optional.empty<List<UByte>>()
                } else {
                    val parsed = parseList(input) { it.toUByteOrNull()?.coerceIn(min, max) }
                    if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
                }
            }

        fun forBoolean(prompt: String) = ChatInputHandler(prompt) { it.toBooleanStrictOrNull() }

        fun forOptionalBoolean(prompt: String) = ChatInputHandler(prompt) { raw ->
            val input = raw.trim()
            if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                Optional.empty<Boolean>()
            } else {
                input.toBooleanStrictOrNull()?.let { Optional.of(it) }
            }
        }

        fun forBooleanList(prompt: String) = ChatInputHandler(prompt) { raw ->
            parseList(raw) { it.toBooleanStrictOrNull() }
        }

        fun forOptionalBooleanList(prompt: String) = ChatInputHandler(prompt) { raw ->
            val input = raw.trim()
            if (input.isBlank() || input.equals("null", ignoreCase = true)) {
                Optional.empty<List<Boolean>>()
            } else {
                val parsed = parseList(input) { it.toBooleanStrictOrNull() }
                if (parsed.isEmpty()) Optional.empty() else Optional.of(parsed)
            }
        }

        fun forMaterial(prompt: String) = forEnum<Material>(prompt)

        fun forKey(prompt: String) = ChatInputHandler(prompt) { str ->
            parseAdventureKey(str)
        }

        fun forOptionalKey(prompt: String) = ChatInputHandler(prompt) { str ->
            val value = str.trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                Optional.empty<Key>()
            } else {
                parseAdventureKey(value)?.let { Optional.of(it) }
            }
        }

        fun forColor(prompt: String) = ChatInputHandler(prompt) { str ->
            parseColor(str)
        }

        fun forOptionalColor(prompt: String) = ChatInputHandler(prompt) { str ->
            val value = str.trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                Optional.empty<Color>()
            } else {
                parseColor(value)?.let { Optional.of(it) }
            }
        }

        fun forItemRarity(prompt: String) = ChatInputHandler(prompt) { str ->
            parseItemRarity(str)
        }

        fun forOptionalItemRarity(prompt: String) = ChatInputHandler(prompt) { str ->
            val value = str.trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                Optional.empty<ItemRarity>()
            } else {
                parseItemRarity(value)?.let { Optional.of(it) }
            }
        }

        fun forEntityType(prompt: String) = ChatInputHandler(prompt) { str ->
            parseEntityType(str)
        }

        fun forOptionalEntityType(prompt: String) = ChatInputHandler(prompt) { str ->
            val value = str.trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                Optional.empty<EntityType>()
            } else {
                parseEntityType(value)?.let { Optional.of(it) }
            }
        }

        fun forItemFlag(prompt: String) = ChatInputHandler(prompt) { str ->
            parseItemFlag(str)
        }

        fun forOptionalItemFlag(prompt: String) = ChatInputHandler(prompt) { str ->
            val value = str.trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                Optional.empty<ItemFlag>()
            } else {
                parseItemFlag(value)?.let { Optional.of(it) }
            }
        }

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

        private fun parseAdventureKey(raw: String): Key? {
            val value = raw.trim()
            if (value.isEmpty()) return null

            val direct = runCatching { Key.key(value) }.getOrNull()
            if (direct != null) return direct

            if (':' !in value) {
                return runCatching { Key.key("minecraft", value.lowercase(Locale.ROOT)) }.getOrNull()
            }
            return null
        }

        private fun parseItemRarity(raw: String): ItemRarity? {
            val value = raw.trim()
            if (value.isEmpty()) return null
            return runCatching { ItemRarity.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
        }

        private fun parseEntityType(raw: String): EntityType? {
            val value = raw.trim()
            if (value.isEmpty()) return null
            return runCatching { EntityType.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
        }

        private fun parseItemFlag(raw: String): ItemFlag? {
            val value = raw.trim()
            if (value.isEmpty()) return null
            return runCatching { ItemFlag.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
        }

        private fun parseColor(raw: String): Color? {
            val value = raw.trim()
            if (value.isEmpty()) return null

            if (value.startsWith("#") && value.length == 7) {
                return runCatching {
                    Color.fromRGB(
                        value.substring(1, 3).toInt(16),
                        value.substring(3, 5).toInt(16),
                        value.substring(5, 7).toInt(16)
                    )
                }.getOrNull()
            }

            val split = if (';' in value) value.split(";") else value.split(",")
            if (split.size != 3) return null

            val red = split[0].trim().toIntOrNull() ?: return null
            val green = split[1].trim().toIntOrNull() ?: return null
            val blue = split[2].trim().toIntOrNull() ?: return null
            return runCatching { Color.fromRGB(red, green, blue) }.getOrNull()
        }

        private fun <T> parseList(raw: String, parser: (String) -> T?): List<T> {
            return raw.split(',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull(parser)
        }
    }
}

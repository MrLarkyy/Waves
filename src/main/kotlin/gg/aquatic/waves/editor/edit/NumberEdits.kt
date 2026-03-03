package gg.aquatic.waves.editor.edit

import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.serialize.BIGDECIMAL
import gg.aquatic.waves.editor.serialize.BIGINT
import gg.aquatic.waves.editor.serialize.BYTE
import gg.aquatic.waves.editor.serialize.DOUBLE
import gg.aquatic.waves.editor.serialize.FLOAT
import gg.aquatic.waves.editor.serialize.INT
import gg.aquatic.waves.editor.serialize.LONG
import gg.aquatic.waves.editor.serialize.SHORT
import gg.aquatic.waves.editor.serialize.UBYTE
import gg.aquatic.waves.editor.serialize.UINT
import gg.aquatic.waves.editor.serialize.ULONG
import gg.aquatic.waves.editor.serialize.USHORT
import gg.aquatic.waves.editor.serialize.ValueSerializer
import gg.aquatic.waves.editor.value.SimpleEditorValue
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.math.BigInteger

fun Configurable<*>.editInt(
    key: String,
    initial: Int,
    prompt: String,
    min: Int? = null,
    max: Int? = null,
    icon: (Int) -> ItemStack = { ItemStack(Material.GOLD_NUGGET).apply { amount = it.coerceIn(1, 64) } }
): SimpleEditorValue<Int> = edit(
    key, initial, ValueSerializer.INT, icon, ChatInputHandler.forInteger(prompt, min, max)
)

fun Configurable<*>.editDouble(
    key: String,
    initial: Double,
    prompt: String,
    min: Double? = null,
    max: Double? = null,
    icon: (Double) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<Double> = edit(
    key, initial, ValueSerializer.DOUBLE, icon, ChatInputHandler.forDouble(prompt, min, max)
)

fun Configurable<*>.editFloat(
    key: String,
    initial: Float,
    prompt: String,
    min: Float? = null,
    max: Float? = null,
    icon: (Float) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<Float> = edit(
    key, initial, ValueSerializer.FLOAT, icon, ChatInputHandler.forFloat(prompt, min, max)
)

fun Configurable<*>.editLong(
    key: String,
    initial: Long,
    prompt: String,
    min: Long? = null,
    max: Long? = null,
    icon: (Long) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<Long> = edit(
    key, initial, ValueSerializer.LONG, icon, ChatInputHandler.forLong(prompt, min, max)
)

fun Configurable<*>.editBigInteger(
    key: String,
    initial: BigInteger,
    prompt: String,
    min: BigInteger? = null,
    max: BigInteger? = null,
    icon: (BigInteger) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<BigInteger> = edit(
    key, initial, ValueSerializer.BIGINT, icon, ChatInputHandler.forBigInt(prompt, min, max)
)

fun Configurable<*>.editBigDecimal(
    key: String,
    initial: BigDecimal,
    prompt: String,
    min: BigDecimal? = null,
    max: BigDecimal? = null,
    icon: (BigDecimal) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<BigDecimal> = edit(
    key, initial, ValueSerializer.BIGDECIMAL, icon, ChatInputHandler.forBigDecimal(prompt, min, max)
)

fun Configurable<*>.editShort(
    key: String,
    initial: Short,
    prompt: String,
    min: Short? = null,
    max: Short? = null,
    icon: (Short) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<Short> = edit(
    key, initial, ValueSerializer.SHORT, icon, ChatInputHandler.forShort(prompt, min, max)
)

fun Configurable<*>.editByte(
    key: String,
    initial: Byte,
    prompt: String,
    min: Byte? = null,
    max: Byte? = null,
    icon: (Byte) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<Byte> = edit(
    key, initial, Serializers.BYTE, icon, ChatInputHandler.forByte(prompt, min, max)
)
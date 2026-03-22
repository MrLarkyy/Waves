package gg.aquatic.waves.editor.edit

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.serialize.*
import gg.aquatic.waves.editor.value.EditorValue
import gg.aquatic.waves.editor.value.ElementBehavior
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.SimpleEditorValue
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

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

fun Configurable<*>.editOptionalInt(
    key: String,
    initial: Optional<Int> = Optional.empty(),
    prompt: String,
    min: Int? = null,
    max: Int? = null,
    icon: (Optional<Int>) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) {
            displayName = Component.text("$key: ${it.map(Int::toString).orElse("unset")}")
        }.getItem()
    }
): SimpleEditorValue<Optional<Int>> = edit(
    key, initial, ValueSerializer.OPTIONAL_INT, icon, ChatInputHandler.forOptionalInteger(prompt, min, max)
)

fun Configurable<*>.editIntList(
    key: String,
    initial: List<Int> = emptyList(),
    prompt: String = "Enter integer value:",
    min: Int? = null,
    max: Int? = null,
    icon: (Int) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    },
    listIcon: (List<EditorValue<Int>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Int> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.INT,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forInteger(prompt, min, max)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.toIntOrNull()?.coerceIn(min, max))
    },
    listIcon = listIcon
)

fun Configurable<*>.editFloatList(
    key: String,
    initial: List<Float> = emptyList(),
    prompt: String = "Enter float value:",
    min: Float? = null,
    max: Float? = null,
    icon: (Float) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    },
    listIcon: (List<EditorValue<Float>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Float> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.FLOAT,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forFloat(prompt, min, max)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.toFloatOrNull()?.coerceIn(min, max))
    },
    listIcon = listIcon
)

fun Configurable<*>.editDoubleList(
    key: String,
    initial: List<Double> = emptyList(),
    prompt: String = "Enter double value:",
    min: Double? = null,
    max: Double? = null,
    icon: (Double) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    },
    listIcon: (List<EditorValue<Double>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Double> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.DOUBLE,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forDouble(prompt, min, max)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.toDoubleOrNull()?.coerceIn(min, max))
    },
    listIcon = listIcon
)

fun Configurable<*>.editLongList(
    key: String,
    initial: List<Long> = emptyList(),
    prompt: String = "Enter long value:",
    min: Long? = null,
    max: Long? = null,
    icon: (Long) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    },
    listIcon: (List<EditorValue<Long>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Long> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.LONG,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forLong(prompt, min, max)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.toLongOrNull()?.coerceIn(min, max))
    },
    listIcon = listIcon
)

fun Configurable<*>.editOptionalIntList(
    key: String,
    initial: Optional<List<Int>> = Optional.empty(),
    prompt: String = "Enter integers separated by ',' or ';' (null to unset):",
    min: Int? = null,
    max: Int? = null
): SimpleEditorValue<Optional<List<Int>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_INT_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalIntegerList(prompt, min, max)
)

fun Configurable<*>.editOptionalFloatList(
    key: String,
    initial: Optional<List<Float>> = Optional.empty(),
    prompt: String = "Enter floats separated by ',' or ';' (null to unset):",
    min: Float? = null,
    max: Float? = null
): SimpleEditorValue<Optional<List<Float>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_FLOAT_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalFloatList(prompt, min, max)
)

fun Configurable<*>.editOptionalDoubleList(
    key: String,
    initial: Optional<List<Double>> = Optional.empty(),
    prompt: String = "Enter doubles separated by ',' or ';' (null to unset):",
    min: Double? = null,
    max: Double? = null
): SimpleEditorValue<Optional<List<Double>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_DOUBLE_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalDoubleList(prompt, min, max)
)

fun Configurable<*>.editOptionalLongList(
    key: String,
    initial: Optional<List<Long>> = Optional.empty(),
    prompt: String = "Enter longs separated by ',' or ';' (null to unset):",
    min: Long? = null,
    max: Long? = null
): SimpleEditorValue<Optional<List<Long>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_LONG_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalLongList(prompt, min, max)
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
    key, initial, ValueSerializer.BYTE, icon, ChatInputHandler.forByte(prompt, min, max)
)

fun Configurable<*>.editUInt(
    key: String,
    initial: UInt,
    prompt: String,
    min: UInt? = null,
    max: UInt? = null,
    icon: (UInt) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<UInt> = edit(
    key, initial, ValueSerializer.UINT, icon, ChatInputHandler.forUInt(prompt, min, max)
)

fun Configurable<*>.editULong(
    key: String,
    initial: ULong,
    prompt: String,
    min: ULong? = null,
    max: ULong? = null,
    icon: (ULong) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<ULong> = edit(
    key, initial, ValueSerializer.ULONG, icon, ChatInputHandler.forULong(prompt, min, max)
)

fun Configurable<*>.editUShort(
    key: String,
    initial: UShort,
    prompt: String,
    min: UShort? = null,
    max: UShort? = null,
    icon: (UShort) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<UShort> = edit(
    key, initial, ValueSerializer.USHORT, icon, ChatInputHandler.forUShort(prompt, min, max)
)

fun Configurable<*>.editUByte(
    key: String,
    initial: UByte,
    prompt: String,
    min: UByte? = null,
    max: UByte? = null,
    icon: (UByte) -> ItemStack = {
        stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
    }
): SimpleEditorValue<UByte> = edit(
    key, initial, ValueSerializer.UBYTE, icon, ChatInputHandler.forUByte(prompt, min, max)
)

fun Configurable<*>.editOptionalDouble(
    key: String,
    initial: Optional<Double> = Optional.empty(),
    prompt: String,
    min: Double? = null,
    max: Double? = null
): SimpleEditorValue<Optional<Double>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_DOUBLE,
    handler = ChatInputHandler.forOptionalDouble(prompt, min, max)
)

fun Configurable<*>.editOptionalFloat(
    key: String,
    initial: Optional<Float> = Optional.empty(),
    prompt: String,
    min: Float? = null,
    max: Float? = null
): SimpleEditorValue<Optional<Float>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_FLOAT,
    handler = ChatInputHandler.forOptionalFloat(prompt, min, max)
)

fun Configurable<*>.editOptionalLong(
    key: String,
    initial: Optional<Long> = Optional.empty(),
    prompt: String,
    min: Long? = null,
    max: Long? = null
): SimpleEditorValue<Optional<Long>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_LONG,
    handler = ChatInputHandler.forOptionalLong(prompt, min, max)
)

fun Configurable<*>.editOptionalShort(
    key: String,
    initial: Optional<Short> = Optional.empty(),
    prompt: String,
    min: Short? = null,
    max: Short? = null
): SimpleEditorValue<Optional<Short>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_SHORT,
    handler = ChatInputHandler.forOptionalShort(prompt, min, max)
)

fun Configurable<*>.editOptionalByte(
    key: String,
    initial: Optional<Byte> = Optional.empty(),
    prompt: String,
    min: Byte? = null,
    max: Byte? = null
): SimpleEditorValue<Optional<Byte>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BYTE,
    handler = ChatInputHandler.forOptionalByte(prompt, min, max)
)

fun Configurable<*>.editOptionalBigInteger(
    key: String,
    initial: Optional<BigInteger> = Optional.empty(),
    prompt: String,
    min: BigInteger? = null,
    max: BigInteger? = null
): SimpleEditorValue<Optional<BigInteger>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BIGINT,
    handler = ChatInputHandler.forOptionalBigInt(prompt, min, max)
)

fun Configurable<*>.editOptionalBigDecimal(
    key: String,
    initial: Optional<BigDecimal> = Optional.empty(),
    prompt: String,
    min: BigDecimal? = null,
    max: BigDecimal? = null
): SimpleEditorValue<Optional<BigDecimal>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BIGDECIMAL,
    handler = ChatInputHandler.forOptionalBigDecimal(prompt, min, max)
)

fun Configurable<*>.editOptionalUInt(
    key: String,
    initial: Optional<UInt> = Optional.empty(),
    prompt: String,
    min: UInt? = null,
    max: UInt? = null
): SimpleEditorValue<Optional<UInt>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_UINT,
    handler = ChatInputHandler.forOptionalUInt(prompt, min, max)
)

fun Configurable<*>.editOptionalULong(
    key: String,
    initial: Optional<ULong> = Optional.empty(),
    prompt: String,
    min: ULong? = null,
    max: ULong? = null
): SimpleEditorValue<Optional<ULong>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_ULONG,
    handler = ChatInputHandler.forOptionalULong(prompt, min, max)
)

fun Configurable<*>.editOptionalUShort(
    key: String,
    initial: Optional<UShort> = Optional.empty(),
    prompt: String,
    min: UShort? = null,
    max: UShort? = null
): SimpleEditorValue<Optional<UShort>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_USHORT,
    handler = ChatInputHandler.forOptionalUShort(prompt, min, max)
)

fun Configurable<*>.editOptionalUByte(
    key: String,
    initial: Optional<UByte> = Optional.empty(),
    prompt: String,
    min: UByte? = null,
    max: UByte? = null
): SimpleEditorValue<Optional<UByte>> = editOptionalNumber(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_UBYTE,
    handler = ChatInputHandler.forOptionalUByte(prompt, min, max)
)

fun Configurable<*>.editShortList(
    key: String,
    initial: List<Short> = emptyList(),
    prompt: String = "Enter short value:",
    min: Short? = null,
    max: Short? = null
): ListEditorValue<Short> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.SHORT,
    handler = ChatInputHandler.forShort(prompt, min, max),
    parser = { it.toShortOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editByteList(
    key: String,
    initial: List<Byte> = emptyList(),
    prompt: String = "Enter byte value:",
    min: Byte? = null,
    max: Byte? = null
): ListEditorValue<Byte> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.BYTE,
    handler = ChatInputHandler.forByte(prompt, min, max),
    parser = { it.toByteOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editBigIntegerList(
    key: String,
    initial: List<BigInteger> = emptyList(),
    prompt: String = "Enter big integer value:",
    min: BigInteger? = null,
    max: BigInteger? = null
): ListEditorValue<BigInteger> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.BIGINT,
    handler = ChatInputHandler.forBigInt(prompt, min, max),
    parser = { it.toBigIntegerOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editBigDecimalList(
    key: String,
    initial: List<BigDecimal> = emptyList(),
    prompt: String = "Enter big decimal value:",
    min: BigDecimal? = null,
    max: BigDecimal? = null
): ListEditorValue<BigDecimal> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.BIGDECIMAL,
    handler = ChatInputHandler.forBigDecimal(prompt, min, max),
    parser = { it.toBigDecimalOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editUIntList(
    key: String,
    initial: List<UInt> = emptyList(),
    prompt: String = "Enter uint value:",
    min: UInt? = null,
    max: UInt? = null
): ListEditorValue<UInt> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.UINT,
    handler = ChatInputHandler.forUInt(prompt, min, max),
    parser = { it.toUIntOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editULongList(
    key: String,
    initial: List<ULong> = emptyList(),
    prompt: String = "Enter ulong value:",
    min: ULong? = null,
    max: ULong? = null
): ListEditorValue<ULong> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.ULONG,
    handler = ChatInputHandler.forULong(prompt, min, max),
    parser = { it.toULongOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editUShortList(
    key: String,
    initial: List<UShort> = emptyList(),
    prompt: String = "Enter ushort value:",
    min: UShort? = null,
    max: UShort? = null
): ListEditorValue<UShort> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.USHORT,
    handler = ChatInputHandler.forUShort(prompt, min, max),
    parser = { it.toUShortOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editUByteList(
    key: String,
    initial: List<UByte> = emptyList(),
    prompt: String = "Enter ubyte value:",
    min: UByte? = null,
    max: UByte? = null
): ListEditorValue<UByte> = editNumberList(
    key = key,
    initial = initial,
    prompt = prompt,
    serializer = ValueSerializer.UBYTE,
    handler = ChatInputHandler.forUByte(prompt, min, max),
    parser = { it.toUByteOrNull()?.coerceIn(min, max) }
)

fun Configurable<*>.editOptionalShortList(
    key: String,
    initial: Optional<List<Short>> = Optional.empty(),
    prompt: String = "Enter shorts separated by ',' or ';' (null to unset):",
    min: Short? = null,
    max: Short? = null
): SimpleEditorValue<Optional<List<Short>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_SHORT_LIST,
    handler = ChatInputHandler.forOptionalShortList(prompt, min, max)
)

fun Configurable<*>.editOptionalByteList(
    key: String,
    initial: Optional<List<Byte>> = Optional.empty(),
    prompt: String = "Enter bytes separated by ',' or ';' (null to unset):",
    min: Byte? = null,
    max: Byte? = null
): SimpleEditorValue<Optional<List<Byte>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BYTE_LIST,
    handler = ChatInputHandler.forOptionalByteList(prompt, min, max)
)

fun Configurable<*>.editOptionalBigIntegerList(
    key: String,
    initial: Optional<List<BigInteger>> = Optional.empty(),
    prompt: String = "Enter big integers separated by ',' or ';' (null to unset):",
    min: BigInteger? = null,
    max: BigInteger? = null
): SimpleEditorValue<Optional<List<BigInteger>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BIGINT_LIST,
    handler = ChatInputHandler.forOptionalBigIntList(prompt, min, max)
)

fun Configurable<*>.editOptionalBigDecimalList(
    key: String,
    initial: Optional<List<BigDecimal>> = Optional.empty(),
    prompt: String = "Enter big decimals separated by ',' or ';' (null to unset):",
    min: BigDecimal? = null,
    max: BigDecimal? = null
): SimpleEditorValue<Optional<List<BigDecimal>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BIGDECIMAL_LIST,
    handler = ChatInputHandler.forOptionalBigDecimalList(prompt, min, max)
)

fun Configurable<*>.editOptionalUIntList(
    key: String,
    initial: Optional<List<UInt>> = Optional.empty(),
    prompt: String = "Enter uints separated by ',' or ';' (null to unset):",
    min: UInt? = null,
    max: UInt? = null
): SimpleEditorValue<Optional<List<UInt>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_UINT_LIST,
    handler = ChatInputHandler.forOptionalUIntList(prompt, min, max)
)

fun Configurable<*>.editOptionalULongList(
    key: String,
    initial: Optional<List<ULong>> = Optional.empty(),
    prompt: String = "Enter ulongs separated by ',' or ';' (null to unset):",
    min: ULong? = null,
    max: ULong? = null
): SimpleEditorValue<Optional<List<ULong>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_ULONG_LIST,
    handler = ChatInputHandler.forOptionalULongList(prompt, min, max)
)

fun Configurable<*>.editOptionalUShortList(
    key: String,
    initial: Optional<List<UShort>> = Optional.empty(),
    prompt: String = "Enter ushorts separated by ',' or ';' (null to unset):",
    min: UShort? = null,
    max: UShort? = null
): SimpleEditorValue<Optional<List<UShort>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_USHORT_LIST,
    handler = ChatInputHandler.forOptionalUShortList(prompt, min, max)
)

fun Configurable<*>.editOptionalUByteList(
    key: String,
    initial: Optional<List<UByte>> = Optional.empty(),
    prompt: String = "Enter ubytes separated by ',' or ';' (null to unset):",
    min: UByte? = null,
    max: UByte? = null
): SimpleEditorValue<Optional<List<UByte>>> = editOptionalNumberList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_UBYTE_LIST,
    handler = ChatInputHandler.forOptionalUByteList(prompt, min, max)
)

private fun <T> Configurable<*>.editOptionalNumber(
    key: String,
    initial: Optional<T>,
    serializer: ValueSerializer<Optional<T>>,
    handler: gg.aquatic.waves.editor.EditorClickHandler<Optional<T>>
): SimpleEditorValue<Optional<T>> = edit(
    key = key,
    initial = initial,
    serializer = serializer,
    icon = {
        stackedItem(Material.GOLD_NUGGET) {
            displayName = Component.text("$key: ${it.map { value -> value.toString() }.orElse("unset")}")
        }.getItem()
    },
    handler = handler
)

private fun <T> Configurable<*>.editNumberList(
    key: String,
    initial: List<T>,
    serializer: ValueSerializer<T>,
    handler: gg.aquatic.waves.editor.EditorClickHandler<T>,
    parser: (String) -> T?,
    prompt: String = "Enter value:"
): ListEditorValue<T> = editList(
    key = key,
    initial = initial,
    serializer = serializer,
    behavior = ElementBehavior(
        icon = {
            stackedItem(Material.GOLD_NUGGET) { displayName = Component.text("$key: $it") }.getItem()
        },
        handler = handler
    ),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.let(parser))
    },
    listIcon = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
)

private fun <T> Configurable<*>.editOptionalNumberList(
    key: String,
    initial: Optional<List<T>>,
    serializer: ValueSerializer<Optional<List<T>>>,
    handler: gg.aquatic.waves.editor.EditorClickHandler<Optional<List<T>>>
): SimpleEditorValue<Optional<List<T>>> = edit(
    key = key,
    initial = initial,
    serializer = serializer,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = handler
)

package gg.aquatic.waves.editor.edit

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.EditorClickHandler
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.serialize.COLOR
import gg.aquatic.waves.editor.serialize.ENTITY_TYPE
import gg.aquatic.waves.editor.serialize.ITEM_FLAG
import gg.aquatic.waves.editor.serialize.ITEM_RARITY
import gg.aquatic.waves.editor.serialize.KEY
import gg.aquatic.waves.editor.serialize.OPTIONAL_COLOR
import gg.aquatic.waves.editor.serialize.OPTIONAL_ENTITY_TYPE
import gg.aquatic.waves.editor.serialize.OPTIONAL_ITEM_RARITY
import gg.aquatic.waves.editor.serialize.OPTIONAL_KEY
import gg.aquatic.waves.editor.serialize.ValueSerializer
import gg.aquatic.waves.editor.value.EditorValue
import gg.aquatic.waves.editor.value.ElementBehavior
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.SimpleEditorValue
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import java.util.Optional

fun Configurable<*>.editKey(
    key: String,
    initial: Key,
    prompt: String,
    icon: (Key) -> ItemStack = {
        stackedItem(Material.NAME_TAG) { displayName = Component.text("$key: ${it.asString()}") }.getItem()
    }
): SimpleEditorValue<Key> = edit(
    key, initial, ValueSerializer.KEY, icon, ChatInputHandler.forKey(prompt)
)

fun Configurable<*>.editOptionalKey(
    key: String,
    initial: Optional<Key> = Optional.empty(),
    prompt: String,
    icon: (Optional<Key>) -> ItemStack = {
        stackedItem(Material.NAME_TAG) { displayName = Component.text("$key: ${it.map(Key::asString).orElse("unset")}") }.getItem()
    }
): SimpleEditorValue<Optional<Key>> = edit(
    key, initial, ValueSerializer.OPTIONAL_KEY, icon, ChatInputHandler.forOptionalKey(prompt)
)

fun Configurable<*>.editItemRarity(
    key: String,
    initial: ItemRarity,
    prompt: String,
    icon: (ItemRarity) -> ItemStack = {
        stackedItem(Material.NETHER_STAR) { displayName = Component.text("$key: ${it.name}") }.getItem()
    }
): SimpleEditorValue<ItemRarity> = edit(
    key, initial, ValueSerializer.ITEM_RARITY, icon, ChatInputHandler.forItemRarity(prompt)
)

fun Configurable<*>.editOptionalItemRarity(
    key: String,
    initial: Optional<ItemRarity> = Optional.empty(),
    prompt: String,
    icon: (Optional<ItemRarity>) -> ItemStack = {
        stackedItem(Material.NETHER_STAR) { displayName = Component.text("$key: ${it.map(ItemRarity::name).orElse("unset")}") }.getItem()
    }
): SimpleEditorValue<Optional<ItemRarity>> = edit(
    key,
    initial,
    ValueSerializer.OPTIONAL_ITEM_RARITY,
    icon,
    ChatInputHandler.forOptionalItemRarity(prompt)
)

fun Configurable<*>.editEntityType(
    key: String,
    initial: EntityType,
    prompt: String,
    icon: (EntityType) -> ItemStack = {
        stackedItem(Material.SPAWNER) { displayName = Component.text("$key: ${it.name}") }.getItem()
    }
): SimpleEditorValue<EntityType> = edit(
    key, initial, ValueSerializer.ENTITY_TYPE, icon, ChatInputHandler.forEntityType(prompt)
)

fun Configurable<*>.editOptionalEntityType(
    key: String,
    initial: Optional<EntityType> = Optional.empty(),
    prompt: String,
    icon: (Optional<EntityType>) -> ItemStack = {
        stackedItem(Material.SPAWNER) { displayName = Component.text("$key: ${it.map(EntityType::name).orElse("unset")}") }.getItem()
    }
): SimpleEditorValue<Optional<EntityType>> = edit(
    key,
    initial,
    ValueSerializer.OPTIONAL_ENTITY_TYPE,
    icon,
    ChatInputHandler.forOptionalEntityType(prompt)
)

fun Configurable<*>.editOptionalColor(
    key: String,
    initial: Optional<Color> = Optional.empty(),
    prompt: String,
    icon: (Optional<Color>) -> ItemStack = {
        stackedItem(Material.LEATHER_CHESTPLATE) { displayName = Component.text("$key: ${it.map(::toHex).orElse("unset")}") }.getItem()
    }
): SimpleEditorValue<Optional<Color>> = edit(
    key, initial, ValueSerializer.OPTIONAL_COLOR, icon, ChatInputHandler.forOptionalColor(prompt)
)

fun Configurable<*>.editColorList(
    key: String,
    initial: List<Color> = emptyList(),
    prompt: String = "Enter color (#RRGGBB or r;g;b):",
    icon: (Color) -> ItemStack = { color ->
        ItemStack(Material.LEATHER_CHESTPLATE).apply {
            editMeta { it.displayName(Component.text(toHex(color))) }
        }
    },
    listIcon: (List<EditorValue<Color>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Colors:").map { it.toMMComponent() })
            lore.addAll(list.map { toHex(it.value).toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Color> = editTypedList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.COLOR,
    parser = { parseColor(it) },
    handler = ChatInputHandler.forColor(prompt),
    icon = icon,
    listIcon = listIcon,
    prompt = prompt
)

fun Configurable<*>.editItemFlagList(
    key: String,
    initial: List<ItemFlag> = emptyList(),
    prompt: String = "Enter item flag (e.g. HIDE_ATTRIBUTES):",
    icon: (ItemFlag) -> ItemStack = {
        stackedItem(Material.COMPARATOR) { displayName = Component.text("$key: ${it.name}") }.getItem()
    },
    listIcon: (List<EditorValue<ItemFlag>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Flags:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.name.toMMComponent() })
        }.getItem()
    }
): ListEditorValue<ItemFlag> = editTypedList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.ITEM_FLAG,
    parser = { parseItemFlag(it) },
    handler = ChatInputHandler.forItemFlag(prompt),
    icon = icon,
    listIcon = listIcon,
    prompt = prompt
)

private fun <T> Configurable<*>.editTypedList(
    key: String,
    initial: List<T>,
    serializer: ValueSerializer<T>,
    parser: (String) -> T?,
    handler: EditorClickHandler<T>,
    icon: (T) -> ItemStack,
    listIcon: (List<EditorValue<T>>) -> ItemStack,
    prompt: String
): ListEditorValue<T> = editList(
    key = key,
    initial = initial,
    serializer = serializer,
    behavior = ElementBehavior(icon = icon, handler = handler),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.let(parser))
    },
    listIcon = listIcon
)

private fun toHex(color: Color): String {
    return "#${"%02X".format(color.red)}${"%02X".format(color.green)}${"%02X".format(color.blue)}"
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

private fun parseItemFlag(raw: String): ItemFlag? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return runCatching { ItemFlag.valueOf(value.uppercase()) }.getOrNull()
}

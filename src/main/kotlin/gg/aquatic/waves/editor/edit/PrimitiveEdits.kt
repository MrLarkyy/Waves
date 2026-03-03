package gg.aquatic.waves.editor.edit

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.serialize.BOOLEAN
import gg.aquatic.waves.editor.serialize.OPTIONAL_BOOLEAN
import gg.aquatic.waves.editor.serialize.OPTIONAL_BOOLEAN_LIST
import gg.aquatic.waves.editor.serialize.OPTIONAL_STRING
import gg.aquatic.waves.editor.serialize.OPTIONAL_STRING_LIST
import gg.aquatic.waves.editor.serialize.ValueSerializer
import gg.aquatic.waves.editor.value.EditorValue
import gg.aquatic.waves.editor.value.ElementBehavior
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.SimpleEditorValue
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.Optional

fun Configurable<*>.editOptionalBoolean(
    key: String,
    initial: Optional<Boolean> = Optional.empty(),
    prompt: String = "Enter boolean (true/false or null):"
): SimpleEditorValue<Optional<Boolean>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BOOLEAN,
    icon = {
        ItemStack(
            when (it.orElse(null)) {
                true -> Material.LIME_DYE
                false -> Material.GRAY_DYE
                null -> Material.BARRIER
            }
        ).apply { editMeta { m -> m.displayName(Component.text("$key: ${it.map(Boolean::toString).orElse("unset")}")) } }
    },
    handler = ChatInputHandler.forOptionalBoolean(prompt)
)

fun Configurable<*>.editOptionalString(
    key: String,
    initial: Optional<String> = Optional.empty(),
    prompt: String = "Enter value (or null):"
): SimpleEditorValue<Optional<String>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_STRING,
    icon = {
        stackedItem(Material.PAPER) {
            displayName = Component.text("$key: ${it.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalString(prompt)
)

fun Configurable<*>.editBooleanList(
    key: String,
    initial: List<Boolean> = emptyList(),
    prompt: String = "Enter boolean value (true/false):",
    icon: (Boolean) -> ItemStack = {
        ItemStack(if (it) Material.LIME_DYE else Material.GRAY_DYE).apply {
            editMeta { m -> m.displayName(Component.text("$key: $it")) }
        }
    },
    listIcon: (List<EditorValue<Boolean>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} values)")
            lore.addAll(listOf("", "Values:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value.toString().toMMComponent() })
        }.getItem()
    }
): ListEditorValue<Boolean> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.BOOLEAN,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forBoolean(prompt)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player)
        accept(input?.toBooleanStrictOrNull())
    },
    listIcon = listIcon
)

fun Configurable<*>.editOptionalBooleanList(
    key: String,
    initial: Optional<List<Boolean>> = Optional.empty(),
    prompt: String = "Enter booleans separated by ',' or ';' (null to unset):"
): SimpleEditorValue<Optional<List<Boolean>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_BOOLEAN_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalBooleanList(prompt)
)

fun Configurable<*>.editOptionalStringList(
    key: String,
    initial: Optional<List<String>> = Optional.empty(),
    prompt: String = "Enter values separated by ',' or ';' (null to unset):"
): SimpleEditorValue<Optional<List<String>>> = edit(
    key = key,
    initial = initial,
    serializer = ValueSerializer.OPTIONAL_STRING_LIST,
    icon = {
        stackedItem(Material.BOOK) {
            displayName = Component.text("$key: ${it.map { list -> "${list.size} values" }.orElse("unset")}")
        }.getItem()
    },
    handler = ChatInputHandler.forOptionalStringList(prompt)
)

package gg.aquatic.waves.editor.edit

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.serialize.COMPONENT
import gg.aquatic.waves.editor.serialize.OPTIONAL_COMPONENT
import gg.aquatic.waves.editor.serialize.ValueSerializer
import gg.aquatic.waves.editor.value.EditorValue
import gg.aquatic.waves.editor.value.ElementBehavior
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.SimpleEditorValue
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.input.impl.ChatInputValidator
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault

fun Configurable<*>.editComponent(
    key: String,
    initial: Component,
    prompt: String,
    icon: (Component) -> ItemStack = {
        stackedItem(Material.NAME_TAG) {
            displayName = "Current: ".toMMComponent().append(it)
        }.getItem()
    }
): SimpleEditorValue<Component> = edit(
    key, initial, ValueSerializer.COMPONENT, icon, ChatInputHandler.forComponent(prompt)
)

fun Configurable<*>.editOptionalComponent(
    key: String,
    initial: Optional<Component>,
    prompt: String,
    icon: (Optional<Component>) -> ItemStack = {
        stackedItem(Material.NAME_TAG) {
            displayName = "Current: ".toMMComponent().append(it.getOrDefault(Component.empty()))
        }.getItem()
    }
): SimpleEditorValue<Optional<Component>> = edit(
    key, initial, ValueSerializer.OPTIONAL_COMPONENT, icon, ChatInputHandler.forOptionalComponent(prompt)
)

fun Configurable<*>.editComponentList(
    key: String,
    initial: List<Component> = emptyList(),
    prompt: String = "Enter line:",
    validator: ChatInputValidator? = null,
    icon: (Component) -> ItemStack = { comp ->
        ItemStack(Material.PAPER).apply { editMeta { it.displayName(comp) } }
    },
    listIcon: (List<EditorValue<Component>>) -> ItemStack = { list ->
        stackedItem(Material.BOOK) {
            displayName = Component.text("Edit $key (${list.size} lines)")
            lore.addAll(listOf("", "Lines:").map { it.toMMComponent() })
            lore.addAll(list.map { it.value })
        }.getItem()
    },
    visibleIf: () -> Boolean = { true }
): ListEditorValue<Component> = editList(
    key = key,
    initial = initial,
    serializer = ValueSerializer.COMPONENT,
    behavior = ElementBehavior(icon = icon, handler = ChatInputHandler.forComponent(prompt)),
    addButtonClick = { player, accept ->
        withContext(BukkitCtx.ofEntity(player)) { player.closeInventory() }
        player.sendMessage(prompt)
        val input = ChatInput.createHandle(validator = validator).await(player)
        accept(input?.toMMComponent())
    },
    listIcon = listIcon,
    visibleIf = visibleIf
)
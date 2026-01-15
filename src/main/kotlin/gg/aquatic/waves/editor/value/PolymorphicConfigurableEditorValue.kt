package gg.aquatic.waves.editor.value

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.EditorHandler.getEditorContext
import gg.aquatic.waves.editor.ValueSerializer
import gg.aquatic.waves.editor.ui.EditorMenuProvider
import gg.aquatic.waves.editor.ui.PolymorphicSelectionMenu
import gg.aquatic.waves.input.impl.ChatInput
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PolymorphicConfigurableEditorValue<T : Configurable<T>>(
    override val key: String,
    override var value: T,
    private val options: Map<String, () -> T>,
    private val iconFactory: (T) -> ItemStack,
    private val selectionMenuTitle: Component,
    override val visibleIf: () -> Boolean = { true },
    override val defaultValue: T? = null
) : EditorValue<T> {

    override val serializer = object : ValueSerializer<T> {
        override fun serialize(section: ConfigurationSection, path: String, value: T) {
            val sub = section.getConfigurationSection(path) ?: section.createSection(path)
            value.serialize(sub)
        }

        override fun deserialize(section: ConfigurationSection, path: String): T {
            val sub = section.getConfigurationSection(path) ?: MemoryConfiguration()
            val instance = value.copy()
            instance.deserialize(sub)
            return instance
        }
    }

    override fun getDisplayItem(): ItemStack = iconFactory(value)

    override fun onClick(player: Player, clickType: ButtonType, updateParent: () -> Unit) {
        val context = player.getEditorContext() ?: return

        when (clickType) {
            ButtonType.LEFT -> openNestedEditor(context, updateParent)
            ButtonType.RIGHT -> openSelectionMenu(player, context, updateParent)
            ButtonType.SHIFT_LEFT -> openChatInput(player, context, updateParent)
            else -> {}
        }
    }

    private fun openNestedEditor(context: EditorContext, updateParent: () -> Unit) {
        KMenuCtx.launch {
            context.navigate {
                EditorMenuProvider.openValueEditor(
                    context = context,
                    title = Component.text("Editing: $key"),
                    values = value.getEditorValues(),
                    onSave = { updateParent() }
                )
            }
        }
    }

    private fun openSelectionMenu(player: Player, context: EditorContext, updateParent: () -> Unit) {
        KMenuCtx.launch {
            context.navigate {
                PolymorphicSelectionMenu(
                    context = context,
                    title = selectionMenuTitle,
                    options = options,
                    onSelect = { newValue ->
                        value = newValue
                        updateParent()
                        // Return to the editor menu after selection
                        KMenuCtx.launch { context.goBack() }
                    }
                ).open(player)
            }
        }
    }

    private fun openChatInput(player: Player, context: EditorContext, updateParent: () -> Unit) {
        player.sendMessage("Enter Type ID (${options.keys.joinToString(", ")}):")
        ChatInput.createHandle().await(player).thenAccept { input ->
            if (input == null) return@thenAccept
            val factory = options[input]
            if (factory != null) {
                value = factory()
                updateParent()
            } else {
                player.sendMessage("§cInvalid type! Available: ${options.keys.joinToString(", ")}")

            }
        }
    }

    override fun clone(): EditorValue<T> = PolymorphicConfigurableEditorValue(key, value.copy(), options, iconFactory, selectionMenuTitle, visibleIf, defaultValue)

    override fun save(section: ConfigurationSection) {
        if (!visibleIf()) return
        val sub = section.getConfigurationSection(key) ?: section.createSection(key)
        value.serialize(sub)
    }

    override fun load(section: ConfigurationSection) {
        val sub = section.getConfigurationSection(key) ?: return
        value.deserialize(sub)
    }
}
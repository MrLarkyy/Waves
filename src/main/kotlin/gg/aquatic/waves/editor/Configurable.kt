package gg.aquatic.waves.editor

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.editor.EditorHandler.getEditorContext
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.handlers.ListGuiHandlerImpl
import gg.aquatic.waves.editor.ui.ConfigurableListMenu
import gg.aquatic.waves.editor.ui.EditorMenuProvider
import gg.aquatic.waves.editor.value.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class Configurable<A : Configurable<A>> {
    private val _editorValues = mutableListOf<EditorValue<*>>()

    /**
     * Returns all registered values in the order they were defined.
     */
    fun getEditorValues(): List<EditorValue<*>> = _editorValues

    protected fun editString(key: String, initial: String, prompt: String) =
        edit(key, initial, Serializers.STRING,
            { ItemStack(Material.PAPER).apply { editMeta { m -> m.displayName(Component.text(it)) } } },
            ChatInputHandler.forString(prompt))

    protected fun editInt(key: String, initial: Int, prompt: String) =
        edit(key, initial, Serializers.INT,
            { ItemStack(Material.GOLD_NUGGET).apply { amount = it.coerceIn(1, 64) } },
            ChatInputHandler.forInteger(prompt))

    protected fun editMaterial(key: String, initial: Material, prompt: String) =
        edit(key, initial, Serializers.MATERIAL, { ItemStack(it) }, ChatInputHandler.forMaterial(prompt))

    /**
     * Unified DSL for any simple value.
     * You define the icon, click logic, and serialization in one place.
     */
    protected fun <T> edit(
        key: String,
        initial: T,
        serializer: ValueSerializer<T>,
        icon: (T) -> ItemStack,
        handler: EditorClickHandler<T>,
        visibleIf: () -> Boolean = { true }
    ): SimpleEditorValue<T> {
        return SimpleEditorValue(
            key,
            initial,
            icon,
            handler,
            { it },
            serializer,
            visibleIf
        ).also { _editorValues.add(it) }
    }

    /**
     * Unified DSL for simple Lists (e.g., List<String>, List<Component>).
     * It automatically wraps simple types into EditorValues.
     */
    protected fun <T> editList(
        key: String,
        initial: List<T> = emptyList(),
        serializer: ValueSerializer<T>,
        behavior: ElementBehavior<T>,
        addButtonClick: (player: Player, accept: (T?) -> Unit) -> Unit,
        listIcon: (List<EditorValue<T>>) -> ItemStack,
        guiHandler: ListGuiHandler<T>,
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<T> {
        val wrapSimple: (T) -> SimpleEditorValue<T> = { valData ->
            SimpleEditorValue("__value", valData, behavior.icon, behavior.handler, { it }, serializer)
        }

        val addButtonClickWrap: (player: Player, accept: (EditorValue<T>?) -> Unit) -> Unit = { p, accept ->
            addButtonClick(p) { accept(it?.let(wrapSimple)) }
        }

        val elementFactory: (ConfigurationSection) -> EditorValue<T> = { section ->
            val initialValue = if (section.contains("__value")) {
                serializer.deserialize(section, "__value")
            } else {
                initial.firstOrNull() ?: throw IllegalStateException("Missing default for $key")
            }
            wrapSimple(initialValue)
        }

        return ListEditorValue(
            key, initial.map(wrapSimple).toMutableList(),
            addButtonClick = addButtonClickWrap,
            iconFactory = listIcon,
            openListGui = { player, editor, update ->
                val context = player.getEditorContext() ?: return@ListEditorValue
                KMenuCtx.launch {
                    context.navigate {
                        ConfigurableListMenu(context, editor, editor.addButtonClick, update).open(player)
                    }
                }
            },
            visibleIf = visibleIf,
            elementFactory = elementFactory
        ).also { _editorValues.add(it) }
    }

    /**
     * Unified DSL for Lists.
     * It handles the wrapping of elements automatically.
     */
    protected fun <T> editList(
        key: String,
        elementFactory: (ConfigurationSection) -> EditorValue<T>,
        addButtonClick: (player: Player, accept: (EditorValue<T>) -> Unit) -> Unit,
        listIcon: (List<EditorValue<T>>) -> ItemStack,
        guiHandler: ListGuiHandler<T>,
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<T> {
        return ListEditorValue(
            key, mutableListOf(), addButtonClick, listIcon, guiHandler,
            visibleIf, elementFactory = elementFactory
        ).also { _editorValues.add(it) }
    }

    /**
     * Specialized DSL for lists of Configurables.
     * @param addButton Logic to create a NEW instance (e.g. via ChatInput).
     * @param factory Logic to create an EMPTY instance (used during deserialization).
     */
    protected fun <T : Configurable<T>> editConfigurableList(
        key: String,
        initial: List<T> = emptyList(),
        factory: () -> T,
        addButton: (Player, (T?) -> Unit) -> Unit = { _, accept -> accept(factory()) },
        listIcon: (List<T>) -> ItemStack,
        itemIcon: (T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<T> {
        val wrapConfigurable: (T) -> ConfigurableEditorValue<T> = { configurable ->
            ConfigurableEditorValue(
                key = "__value",
                value = configurable,
                iconFactory = { itemIcon(it) }
            )
        }

        val elementFactory: (ConfigurationSection) -> EditorValue<T> = { section ->
            val instance = factory()
            instance.deserialize(section)
            wrapConfigurable(instance)
        }

        return ListEditorValue(
            key = key,
            value = initial.map(wrapConfigurable).toMutableList(),
            addButtonClick = { player, accept ->
                addButton(player) { newInstance ->
                    accept(newInstance?.let { wrapConfigurable(it) })
                }
            },
            iconFactory = { list -> listIcon(list.map { it.value }) },
            openListGui = { player, editor, update ->
                val context = player.getEditorContext() ?: return@ListEditorValue
                KMenuCtx.launch {
                    context.navigate {
                        ConfigurableListMenu(context, editor, editor.addButtonClick, update).open(player)
                    }
                }
            },
            visibleIf = visibleIf,
            elementFactory = elementFactory
        ).also { _editorValues.add(it) }
    }

    /**
     * Specialized DSL for Maps of Configurables (ConfigurationSection in YAML).
     */
    protected fun <T : Configurable<T>> editConfigurableMap(
        key: String,
        initial: Map<String, T> = emptyMap(),
        factory: () -> T,
        addButton: (Player, (key: String?, value: T?) -> Unit) -> Unit,
        listIcon: (Map<String, T>) -> ItemStack,
        itemIcon: (key: String, value: T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): MapEditorValue<T> {
        val wrapEntry: (String, T) -> ConfigurableEditorValue<T> = { entryKey, configurable ->
            ConfigurableEditorValue(
                key = entryKey,
                value = configurable,
                iconFactory = { itemIcon(entryKey, it) }
            )
        }

        val elementFactory: (ConfigurationSection) -> EditorValue<T> = { section ->
            val instance = factory()
            instance.deserialize(section)
            wrapEntry(section.name, instance)
        }

        return MapEditorValue(
            key = key,
            value = initial.map { (k, v) -> wrapEntry(k, v) }.toMutableList(),
            addButtonClick = { player, accept ->
                addButton(player) { newKey, newInstance ->
                    if (newKey != null && newInstance != null) {
                        accept(wrapEntry(newKey, newInstance))
                    } else {
                        accept(null)
                    }
                }
            },
            iconFactory = listIcon,
            openMapGui = { player, editor, update ->
                val context = player.getEditorContext() ?: return@MapEditorValue
                KMenuCtx.launch {
                    context.navigate {
                        ConfigurableListMenu(context, editor, editor.addButtonClick, update).open(player)
                    }
                }
            },
            visibleIf = visibleIf,
            elementFactory = elementFactory
        ).also { _editorValues.add(it) }
    }

    fun serialize(section: ConfigurationSection) {
        getEditorValues().forEach { it.save(section) }
    }

    fun deserialize(section: ConfigurationSection) {
        getEditorValues().forEach { it.load(section) }
    }

    @Suppress("UNCHECKED_CAST")
    fun asEditorValue(
        key: String,
        icon: (A) -> ItemStack
    ): EditorValue<A> {
        return ConfigurableEditorValue(key, this as A, icon)
    }

    protected fun <T> openListMenu(
        player: Player,
        editor: EditorValue<MutableList<EditorValue<T>>>,
        addLogic: (Player, (EditorValue<T>?) -> Unit) -> Unit,
        update: () -> Unit
    ) {
        val context = player.getEditorContext() ?: return
        KMenuCtx.launch {
            context.navigate {
                ConfigurableListMenu(context, editor, addLogic, update).open(player)
            }
        }
    }

    /**
     * Requirement: Configurables must be able to deep-copy themselves for the editor's "cancel" logic.
     */
    abstract fun copy(): A


}
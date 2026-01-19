package gg.aquatic.waves.editor

import gg.aquatic.common.getSectionList
import gg.aquatic.common.toMMComponent
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.EditorHandler.getEditorContext
import gg.aquatic.waves.editor.handlers.ChatInputHandler
import gg.aquatic.waves.editor.ui.ConfigurableListMenu
import gg.aquatic.waves.editor.ui.PolymorphicSelectionMenu
import gg.aquatic.waves.editor.value.*
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.input.impl.ChatInputValidator
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class Configurable<A : Configurable<A>> {
    private val _editorValues = mutableListOf<EditorValue<*>>()

    /**
     * Returns all registered values in the order they were defined.
     */
    fun getEditorValues(): List<EditorValue<*>> = _editorValues

    /**
     * Creates a read-only string editor value with a custom icon factory.
     * This string still appears in the GUI, but cannot be edited.
     */
    protected fun infoString(
        key: String, initial: String, icon: (String) -> ItemStack = {
            ItemStack(Material.PAPER).apply { editMeta { m -> m.displayName(Component.text(it)) } }
        }
    ) = edit(
        key = key,
        initial = initial,
        serializer = Serializers.STRING,
        icon = icon,
        handler = { _, _, _, _ -> /* Do nothing, it's read-only */ }
    )

    protected fun editString(key: String, initial: String, prompt: String) =
        edit(
            key, initial, Serializers.STRING,
            { ItemStack(Material.PAPER).apply { editMeta { m -> m.displayName(Component.text(it)) } } },
            ChatInputHandler.forString(prompt)
        )

    protected fun editStringList(
        key: String,
        initial: List<String> = emptyList(),
        prompt: String = "Enter text:",
        validator: ChatInputValidator? = null,
        icon: (String) -> ItemStack = { str ->
            ItemStack(Material.PAPER).apply {
                editMeta { it.displayName(Component.text(str)) }
            }
        },
        listIcon: (List<EditorValue<String>>) -> ItemStack = { list ->
            stackedItem(Material.BOOK) {
                displayName = Component.text("Edit $key (${list.size} lines")
                lore.addAll(listOf("", "Lines:").map { it.toMMComponent() })
                lore.addAll(list.map { it.value.toMMComponent() })
            }.getItem()
        },
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<String> {
        return editList(
            key = key,
            initial = initial,
            serializer = Serializers.STRING,
            behavior = ElementBehavior(
                icon = icon,
                handler = ChatInputHandler.forString(prompt)
            ),
            addButtonClick = { player, accept ->
                player.closeInventory()
                player.sendMessage(prompt)
                ChatInput.createHandle(validator = validator).await(player).thenAccept {
                    accept(it)
                }
            },
            listIcon = listIcon,
            visibleIf = visibleIf
        )
    }

    protected inline fun <reified T : Enum<T>> editEnum(
        key: String,
        initial: T,
        prompt: String,
        noinline icon: (T) -> ItemStack = {
            ItemStack(Material.COMPARATOR).apply {
                editMeta { m -> m.displayName(Component.text("$key: ${it.name}")) }
            }
        }
    ) = edit(
        key = key,
        initial = initial,
        serializer = ValueSerializer.EnumSerializer(T::class.java),
        icon = icon,
        handler = ChatInputHandler.forEnum<T>(prompt)
    )

    protected fun editComponentList(
        key: String,
        initial: List<Component> = emptyList(),
        prompt: String = "Enter line:",
        validator: ChatInputValidator? = null,
        icon: (Component) -> ItemStack = { comp ->
            ItemStack(Material.PAPER).apply {
                editMeta { it.displayName(comp) }
            }
        },
        listIcon: (List<EditorValue<Component>>) -> ItemStack = { list ->
            stackedItem(Material.BOOK) {
                displayName = Component.text("Edit $key (${list.size} lines")
                lore.addAll(listOf("", "Lines:").map { it.toMMComponent() })
                lore.addAll(list.map { it.value })
            }.getItem()
        },
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<Component> {
        return editList(
            key = key,
            initial = initial,
            serializer = Serializers.COMPONENT,
            behavior = ElementBehavior(
                icon = icon,
                handler = ChatInputHandler.forComponent(prompt)
            ),
            addButtonClick = { player, accept ->
                player.closeInventory()
                player.sendMessage(prompt)
                ChatInput.createHandle(validator = validator).await(player).thenAccept {
                    accept(it?.toMMComponent())
                }
            },
            listIcon = listIcon,
            visibleIf = visibleIf
        )
    }

    protected fun editBoolean(
        key: String,
        initial: Boolean,
        icon: (Boolean) -> ItemStack = {
            ItemStack(if (it) Material.LIME_DYE else Material.GRAY_DYE).apply {
                editMeta { m -> m.displayName(Component.text("$key: $it")) }
            }
        }
    ) = edit(
        key = key,
        initial = initial,
        serializer = Serializers.BOOLEAN,
        icon = icon,
        handler = { _, editor, _, update ->
            update(!editor)
        }
    )

    /**
     * Specialized DSL for Map<String, List<T>> where T is polymorphic (Action, HologramLine, etc.)
     */
    protected fun <T : Configurable<T>> editString2PolymorphicListConfigurableMap(
        key: String,
        initial: Map<String, List<T>> = emptyMap(),
        options: Map<String, () -> T>,
        addButton: (Player, (key: String?) -> Unit) -> Unit,
        mapIcon: (Map<String, List<T>>) -> ItemStack,
        listIcon: (key: String, List<T>) -> ItemStack,
        itemIcon: (T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): MapEditorValue<MutableList<EditorValue<T>>> {

        // Helper to create the inner ListEditorValue using the existing polymorphic logic
        val createInnerList: (String, List<T>) -> ListEditorValue<T> = { listKey, listValues ->
            // Temporarily remove from _editorValues because editPolymorphicConfigurableList adds itself there
            val listEditor = editPolymorphicConfigurableList(
                key = listKey,
                initial = listValues,
                options = options,
                listIcon = { listIcon(listKey, it) },
                itemIcon = itemIcon
            )
            _editorValues.remove(listEditor)
            listEditor
        }

        return MapEditorValue(
            key = key,
            value = initial.map { (k, v) -> createInnerList(k, v) }.toMutableList(),
            addButtonClick = { player, accept ->
                addButton(player) { newKey ->
                    if (newKey != null) accept(createInnerList(newKey, emptyList()))
                    else accept(null)
                }
            },
            iconFactory = { editorValues ->
                val dataMap = HashMap<String, List<T>>()
                for (editorValue in editorValues) {
                    dataMap[editorValue.key] = editorValue.value.map { it.value }
                }
                mapIcon(dataMap)
            },
            openMapGui = { player, editor, update -> openListMenu(player, editor, editor.addButtonClick, update) },
            visibleIf = visibleIf,
            elementFactory = { section ->
                val instanceList = section.getSectionList("").map { sec ->
                    val instance = options.values.first()().copy()
                    instance.deserialize(sec)
                    instance
                }
                createInnerList(section.name, instanceList)
            }
        ).also { _editorValues.add(it) }
    }

    /**
     * Specialized DSL for Map<Int, List<T>> where T is polymorphic.
     */
    protected fun <T : Configurable<T>> editInt2PolymorphicListConfigurableMap(
        key: String,
        initial: Map<Int, List<T>> = emptyMap(),
        options: Map<String, () -> T>,
        addButton: (Player, (key: Int?) -> Unit) -> Unit,
        mapIcon: (Map<Int, List<T>>) -> ItemStack,
        listIcon: (key: Int, List<T>) -> ItemStack,
        itemIcon: (T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): MapEditorValue<MutableList<EditorValue<T>>> {
        return editString2PolymorphicListConfigurableMap(
            key = key,
            initial = initial.mapKeys { it.key.toString() },
            options = options,
            addButton = { player, accept ->
                addButton(player) { intKey -> accept(intKey?.toString()) }
            },
            mapIcon = { stringMap ->
                val intMap = HashMap<Int, List<T>>()
                for (entry in stringMap) {
                    val intKey = entry.key.toIntOrNull() ?: continue
                    intMap[intKey] = entry.value
                }
                mapIcon(intMap)
            },
            listIcon = { k, list -> listIcon(k.toIntOrNull() ?: 0, list) },
            itemIcon = itemIcon,
            visibleIf = visibleIf
        )
    }

    protected fun editInt(key: String, initial: Int, prompt: String) =
        edit(
            key, initial, Serializers.INT,
            { ItemStack(Material.GOLD_NUGGET).apply { amount = it.coerceIn(1, 64) } },
            ChatInputHandler.forInteger(prompt)
        )

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

    protected fun <T : Configurable<T>> editConfigurable(
        key: String,
        initial: T,
        icon: (T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): ConfigurableEditorValue<T> {
        return ConfigurableEditorValue(
            key = key,
            value = initial,
            iconFactory = icon,
            visibleIf = visibleIf
        ).also { _editorValues.add(it) }
    }

    protected fun <T : Configurable<T>> editPolymorphicConfigurable(
        key: String,
        initial: T,
        options: Map<String, () -> T>,
        icon: (T) -> ItemStack,
        menuTitle: Component = Component.text("Select Type"),
        visibleIf: () -> Boolean = { true }
    ): PolymorphicConfigurableEditorValue<T> {
        return PolymorphicConfigurableEditorValue(
            key = key,
            value = initial,
            options = options,
            iconFactory = icon,
            selectionMenuTitle = menuTitle,
            visibleIf = visibleIf
        ).also { _editorValues.add(it) }
    }

    /**
     * DSL for a list of Configurables where each element can be a different type.
     * When adding a new element, it opens a selection menu to pick the type.
     */
    protected fun <T : Configurable<T>> editPolymorphicConfigurableList(
        key: String,
        initial: List<T> = emptyList(),
        options: Map<String, () -> T>,
        listIcon: (List<T>) -> ItemStack,
        itemIcon: (T) -> ItemStack,
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<T> {
        val wrapPolymorphic: (T) -> PolymorphicConfigurableEditorValue<T> = { configurable ->
            PolymorphicConfigurableEditorValue(
                key = "__value",
                value = configurable,
                options = options,
                iconFactory = { itemIcon(it) },
                selectionMenuTitle = Component.text("Select Type")
            )
        }

        val elementFactory: (ConfigurationSection) -> EditorValue<T> = { section ->
            // Note: For polymorphic loading, we'd ideally need a type hint in the section.
            // Assuming the base factory or a copy-logic can handle it via deserialize.
            val instance = options.values.first()().copy()
            instance.deserialize(section)
            wrapPolymorphic(instance)
        }

        return ListEditorValue(
            key = key,
            value = initial.map(wrapPolymorphic).toMutableList(),
            addButtonClick = { player, accept ->
                val context = player.getEditorContext() ?: return@ListEditorValue
                KMenuCtx.launch {
                    context.navigate {
                        PolymorphicSelectionMenu(
                            context = context,
                            title = Component.text("Select Type to Add"),
                            options = options,
                            onSelect = { newInstance ->
                                accept(wrapPolymorphic(newInstance))
                                KMenuCtx.launch { context.goBack() }
                            }
                        ).open(player)
                    }
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
        guiHandler: ListGuiHandler<T>? = null,
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

        val finalGuiHandler = guiHandler ?: ListGuiHandler { player, editor, update ->
            val context = player.getEditorContext() ?: return@ListGuiHandler
            KMenuCtx.launch {
                context.navigate {
                    ConfigurableListMenu(context, editor, editor.addButtonClick, update).open(player)
                }
            }
        }

        return ListEditorValue(
            key, initial.map(wrapSimple).toMutableList(),
            addButtonClick = addButtonClickWrap,
            iconFactory = listIcon,
            openListGui = finalGuiHandler,
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
        guiHandler: ListGuiHandler<T>? = null,
        visibleIf: () -> Boolean = { true }
    ): ListEditorValue<T> {
        val finalGuiHandler = guiHandler ?: ListGuiHandler { player, editor, update ->
            val context = player.getEditorContext() ?: return@ListGuiHandler
            KMenuCtx.launch {
                context.navigate {
                    ConfigurableListMenu(context, editor, editor.addButtonClick, update).open(player)
                }
            }
        }

        return ListEditorValue(
            key, mutableListOf(), addButtonClick, listIcon, finalGuiHandler,
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
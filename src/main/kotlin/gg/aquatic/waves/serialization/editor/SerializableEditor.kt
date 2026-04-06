package gg.aquatic.waves.serialization.editor

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalSerializationApi::class)
object SerializableEditor {

    private val defaultYaml = Yaml()

    fun <T> startEditing(
        player: Player,
        title: Component,
        serializer: KSerializer<T>,
        yaml: Yaml = defaultYaml,
        schema: EditorSchema<T>? = null,
        loadFresh: () -> T,
        onSave: (T) -> Unit
    ) {
        val context = EditorContext(player)
        val document = SerializableEditorDocument(yaml, serializer, loadFresh())

        KMenu.scope.launch {
            context.navigate {
                openNodeEditor(
                    context = context,
                    title = title,
                    document = document,
                    path = emptyList(),
                    descriptor = serializer.descriptor,
                    label = titleString(title),
                    schema = schema
                ) {
                    val decoded = runCatching { document.decode() }
                    decoded.onSuccess {
                        onSave(it)
                        player.sendMessage("Changes saved successfully!")
                    }.onFailure {
                        player.sendMessage("Failed to save: ${it.message ?: it.javaClass.simpleName}")
                    }
                }
            }
        }
    }

    suspend fun <T> editValue(
        player: Player,
        title: Component,
        serializer: KSerializer<T>,
        yaml: Yaml = defaultYaml,
        schema: EditorSchema<T>? = null,
        loadFresh: () -> T
    ): T? = suspendCancellableCoroutine { continuation ->
        val context = EditorContext(player)
        val document = SerializableEditorDocument(yaml, serializer, loadFresh())
        var completed = false

        fun complete(result: T?) {
            if (completed || !continuation.isActive) return
            completed = true
            continuation.resume(result)
        }

        KMenu.scope.launch {
            context.navigate {
                openNodeEditor(
                    context = context,
                    title = title,
                    document = document,
                    path = emptyList(),
                    descriptor = serializer.descriptor,
                    label = titleString(title),
                    schema = schema,
                    onReturn = { complete(null) },
                    onClosed = {
                        if (!context.shouldIgnoreClosedMenu()) {
                            complete(null)
                        }
                    }
                ) {
                    complete(runCatching { document.decode() }.getOrNull())
                }
            }
        }
    }

    private suspend fun <T> openNodeEditor(
        context: EditorContext,
        title: Component,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        descriptor: SerialDescriptor,
        label: String,
        schema: EditorSchema<T>?,
        onReturn: (suspend () -> Unit)? = null,
        onClosed: (suspend () -> Unit)? = null,
        onSave: suspend () -> Unit
    ) {
        val editorState = prepareNodeEditorState(document, path, descriptor, label, schema)
        passthroughEntry(path, editorState.resolvedDescriptor, editorState.entries)?.let { passthrough ->
            openNodeEditor(
                context = context,
                title = title,
                document = document,
                path = passthrough.path,
                descriptor = passthrough.descriptor,
                label = passthrough.label,
                schema = schema,
                onReturn = onReturn,
                onClosed = onClosed,
                onSave = onSave
            )
            return
        }
        val menuTitle = if (path.isEmpty()) title else Component.text(label.take(32))
        val entrySlots = (0..44).toList()

        context.player.createMenu(menuTitle, InventoryType.GENERIC9X6) {
            if (onClosed != null) {
                menuFactory = { customTitle, customType, customPlayer, cancelInteractions ->
                    object : PrivateMenu(customTitle, customType, customPlayer, cancelInteractions) {
                        override suspend fun onClosed(player: Player) {
                            onClosed()
                        }
                    }
                }
            }
            editorState.entries.take(entrySlots.size).forEachIndexed { index, entry ->
                button("entry_${index}_${entry.label}", entrySlots[index]) {
                    item = entryIcon(entry)
                    onClick { event ->
                        handleEntryClick(context, title, document, entry, schema, onSave, event.buttonType)
                    }
                }
            }

            addEditorControls(
                context,
                document,
                path,
                label,
                descriptor,
                editorState.resolvedDescriptor,
                schema,
                onSave,
                onReturn,
                hasCloseHandler = onClosed != null
            )
        }.open(context.player)
    }

    private suspend fun <T> handleEntryClick(
        context: EditorContext,
        title: Component,
        document: SerializableEditorDocument<T>,
        entry: EditorEntry,
        schema: EditorSchema<T>?,
        onSave: suspend () -> Unit,
        buttonType: ButtonType
    ) {
        if (buttonType == ButtonType.DROP && handleDropAction(document, entry, context)) {
            return
        }

        if (handleAdapterAction(document, entry, context, buttonType)) {
            return
        }

        when (entry.kind) {
            NodeKind.OBJECT, NodeKind.LIST, NodeKind.MAP -> context.navigate {
                openNodeEditor(context, title, document, entry.path, entry.descriptor, entry.label, schema, onReturn = null, onClosed = null, onSave)
            }

            NodeKind.BOOLEAN -> {
                val currentValue = document.get(entry.path).booleanOrNull ?: false
                document.set(entry.path, yamlScalar(!currentValue))
                context.refresh()
            }

            NodeKind.STRING, NodeKind.NUMBER -> {
                editPrimitiveValue(context.player, document, entry)
                context.refresh()
            }

            NodeKind.ENUM -> {
                editEnumValue(context.player, document, entry)
                context.refresh()
            }
        }
    }

    private suspend fun <T> handleDropAction(
        document: SerializableEditorDocument<T>,
        entry: EditorEntry,
        context: EditorContext
    ): Boolean {
        return when {
            entry.removable -> {
                document.remove(entry.path)
                context.refresh()
                true
            }

            entry.descriptor.isNullable -> {
                document.set(entry.path, yamlNull())
                context.refresh()
                true
            }

            else -> false
        }
    }

    private suspend fun <T> handleAdapterAction(
        document: SerializableEditorDocument<T>,
        entry: EditorEntry,
        context: EditorContext,
        buttonType: ButtonType
    ): Boolean {
        val adapter = entry.meta?.adapter
        if (adapter == null || adapter === DefaultEditorFieldAdapter) {
            return false
        }

        return when (val result = adapter.edit(context.player, entry.toFieldContext(document.root()), buttonType)) {
            is FieldEditResult.Updated -> {
                document.set(entry.path, result.value)
                context.refresh()
                true
            }

            is FieldEditResult.UpdatedRoot -> {
                document.replaceRoot(result.value)
                context.refresh()
                true
            }

            FieldEditResult.NoChange -> {
                context.refresh()
                true
            }

            FieldEditResult.PassThrough -> false
        }
    }

    private fun <T> gg.aquatic.kmenu.menu.PrivateMenuBuilder.addEditorControls(
        context: EditorContext,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        label: String,
        descriptor: SerialDescriptor,
        resolvedDescriptor: SerialDescriptor,
        schema: EditorSchema<T>?,
        onSave: suspend () -> Unit,
        onReturn: (suspend () -> Unit)?,
        hasCloseHandler: Boolean,
    ) {
        button("save", 45) {
            item = stackedItem(Material.LIME_DYE) {
                displayName = Component.text("Save")
            }.getItem()
            onClick { onSave() }
        }

        if (path.isNotEmpty()) {
            button("back", 46) {
                item = stackedItem(Material.ARROW) {
                    displayName = Component.text("Back")
                }.getItem()
                onClick { context.goBack() }
            }
        } else if (onReturn != null) {
            button("return", 46) {
                item = stackedItem(Material.ARROW) {
                    displayName = Component.text("Return")
                }.getItem()
                onClick { onReturn() }
            }
        } else if (hasCloseHandler) {
            button("close", 46) {
                item = stackedItem(Material.BARRIER) {
                    displayName = Component.text("Close")
                }.getItem()
                onClick {
                    withContext(BukkitCtx.ofEntity(context.player)) {
                        context.player.closeInventory()
                    }
                }
            }
        }

        if (resolvedDescriptor.kind == StructureKind.LIST) {
            button("add_list", 47) {
                item = addEntryItem()
                onClick {
                    addListEntry(context.player, document, path, label, descriptor, resolvedDescriptor, schema)
                    context.refresh()
                }
            }
        }

        if (resolvedDescriptor.kind == StructureKind.MAP) {
            button("add_map", 47) {
                item = addEntryItem()
                onClick {
                    addMapEntry(context.player, document, path, resolvedDescriptor, schema, label)
                    context.refresh()
                }
            }
        }
    }

    private fun addEntryItem() = stackedItem(Material.NETHER_STAR) {
        displayName = Component.text("Add Entry")
    }.getItem()

    private suspend fun <T> addListEntry(
        player: Player,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        label: String,
        descriptor: SerialDescriptor,
        resolvedDescriptor: SerialDescriptor,
        schema: EditorSchema<T>?
    ) {
        val contextValue = document.get(path)
        val baseFieldContext = buildFieldContext(label, path, descriptor, contextValue, document.root())
        val containerMeta = schema?.resolve(baseFieldContext)
        val fieldContext = baseFieldContext.copy(description = containerMeta?.description.orEmpty())
        val created = if (containerMeta?.newValueFactory != null) {
            containerMeta.newValueFactory.create(player, fieldContext) ?: return
        } else {
            defaultYamlElement(resolvedDescriptor.getElementDescriptor(0), useNullForNullable = false)
        }
        if (created is YamlList && resolvedDescriptor.getElementDescriptor(0).kind != StructureKind.LIST) {
            document.addAllToList(path, created)
        } else {
            document.addToList(path, created)
        }
    }

    private suspend fun <T> addMapEntry(
        player: Player,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        descriptor: SerialDescriptor,
        schema: EditorSchema<T>?,
        label: String
    ) {
        val fieldContext = EditorFieldContext(
            label = label,
            path = path.toSchemaPath(),
            pathSegments = path.toSchemaSegments(),
            description = emptyList(),
            descriptor = descriptor,
            value = document.get(path),
            root = document.root()
        )
        val containerMeta = schema?.resolve(fieldContext)
        val resolvedFieldContext = fieldContext.copy(description = containerMeta?.description.orEmpty())

        val custom = containerMeta?.newMapEntryFactory?.create(player, resolvedFieldContext)
        if (custom != null) {
            document.putToMap(path, custom.first, custom.second)
            return
        }

        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }

        EditorChatMessages.sendPrompt(player, containerMeta?.mapKeyPrompt ?: "Enter entry key:")
        val key = ChatInput.createHandle(listOf("cancel")).await(player)?.trim().orEmpty()
        if (key.isEmpty()) return

        document.putToMap(
            path = path,
            key = key,
            value = defaultYamlElement(descriptor.getElementDescriptor(1), useNullForNullable = false)
        )
    }
    private fun titleString(component: Component): String {
        return component.toString()
    }
}

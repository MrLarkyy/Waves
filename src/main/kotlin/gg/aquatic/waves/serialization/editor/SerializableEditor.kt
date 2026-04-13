package gg.aquatic.waves.serialization.editor

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.inventory.PacketInventory
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.kmenu.packetInventory
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
    private const val SEARCH_PREFIX = "Search: "

    private data class SearchResult(
        val entry: EditorEntry,
        val location: String,
        val tags: List<String>,
        val searchText: String
    )

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
                    schema = schema,
                    showSaveButton = true
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
    ): T? = editValueWithContext(
        context = EditorContext(player),
        title = title,
        serializer = serializer,
        yaml = yaml,
        schema = schema,
        loadFresh = loadFresh,
    )

    suspend fun <T> editValueInActiveContext(
        player: Player,
        title: Component,
        serializer: KSerializer<T>,
        yaml: Yaml = defaultYaml,
        schema: EditorSchema<T>? = null,
        loadFresh: () -> T
    ): T? {
        val activeContext = ActiveEditorContextRegistry.get(player) ?: return editValue(
            player = player,
            title = title,
            serializer = serializer,
            yaml = yaml,
            schema = schema,
            loadFresh = loadFresh,
        )

        return editValueWithContext(
            context = activeContext,
            title = title,
            serializer = serializer,
            yaml = yaml,
            schema = schema,
            loadFresh = loadFresh,
        )
    }

    suspend fun suppressActiveContextClose(player: Player) {
        ActiveEditorContextRegistry.get(player)?.suppressNextCloseEvent()
    }


    private suspend fun <T> editValueWithContext(
        context: EditorContext,
        title: Component,
        serializer: KSerializer<T>,
        yaml: Yaml,
        schema: EditorSchema<T>?,
        loadFresh: () -> T
    ): T? = suspendCancellableCoroutine { continuation ->
        val document = SerializableEditorDocument(yaml, serializer, loadFresh())
        var completed = false

        fun complete(result: T?) {
            if (completed || !continuation.isActive) return
            completed = true
            KMenu.scope.launch {
                context.dismissCurrent()
                continuation.resume(result)
            }
        }

        fun decodeDraft(): T? = runCatching { document.decode() }.getOrNull()

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
                    onReturn = { complete(decodeDraft()) },
                    onClosed = {
                        if (!context.shouldIgnoreClosedMenu()) {
                            complete(decodeDraft())
                        }
                    },
                    showSaveButton = false
                ) {
                    complete(decodeDraft())
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
        showSaveButton: Boolean,
        onSave: suspend () -> Unit
    ) {
        val editorState = prepareNodeEditorState(document, path, descriptor, label, schema)
        val currentNode = document.get(path)
        passthroughEntry(path, editorState.resolvedDescriptor, editorState.entries, currentNode)?.let { passthrough ->
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
                showSaveButton = showSaveButton,
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
                        handleEntryClick(context, title, document, entry, schema, onSave, event.buttonType, showSaveButton)
                    }
                }
            }

            addEditorControls(
                context,
                title,
                document,
                path,
                label,
                descriptor,
                editorState.resolvedDescriptor,
                editorState.entries,
                schema,
                onSave,
                onReturn,
                hasCloseHandler = onClosed != null,
                showSaveButton = showSaveButton
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
        buttonType: ButtonType,
        showSaveButton: Boolean
    ) {
        if (buttonType == ButtonType.DROP && handleDropAction(document, entry, context)) {
            return
        }

        if (handleAdapterAction(document, entry, context, buttonType)) {
            return
        }

        when (entry.kind) {
            NodeKind.OBJECT, NodeKind.LIST, NodeKind.MAP -> context.navigate {
                openNodeEditor(
                    context,
                    title,
                    document,
                    entry.path,
                    entry.descriptor,
                    entry.label,
                    schema,
                    onReturn = null,
                    onClosed = null,
                    showSaveButton = showSaveButton,
                    onSave = onSave
                )
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

        return ActiveEditorContextRegistry.withContext(context.player, context) {
            when (val result = adapter.edit(context.player, entry.toFieldContext(document.root()), buttonType)) {
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
    }

    private fun <T> gg.aquatic.kmenu.menu.PrivateMenuBuilder.addEditorControls(
        context: EditorContext,
        title: Component,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        label: String,
        descriptor: SerialDescriptor,
        resolvedDescriptor: SerialDescriptor,
        entries: List<EditorEntry>,
        schema: EditorSchema<T>?,
        onSave: suspend () -> Unit,
        onReturn: (suspend () -> Unit)?,
        hasCloseHandler: Boolean,
        showSaveButton: Boolean,
    ) {
        val returnSlot = if (showSaveButton) 46 else 45
        val addEntrySlot = if (showSaveButton) 47 else 46

        if (showSaveButton) {
            button("save", 45) {
                item = stackedItem(Material.LIME_DYE) {
                    displayName = Component.text("Save")
                }.getItem()
                onClick { onSave() }
            }
        }

        if (path.isNotEmpty()) {
            button("back", returnSlot) {
                item = stackedItem(Material.ARROW) {
                    displayName = Component.text("Back")
                }.getItem()
                onClick { context.goBack() }
            }
        } else if (onReturn != null) {
            button("return", returnSlot) {
                item = stackedItem(Material.ARROW) {
                    displayName = Component.text("Return")
                }.getItem()
                onClick { onReturn() }
            }
        } else if (hasCloseHandler) {
            button("close", returnSlot) {
                item = stackedItem(Material.BARRIER) {
                    displayName = Component.text("Close")
                }.getItem()
                onClick {
                    EditorCloseGuard.suppress(context.player)
                    withContext(BukkitCtx.ofEntity(context.player)) {
                        context.player.closeInventory()
                    }
                }
            }
        }

        if (resolvedDescriptor.kind == StructureKind.LIST) {
            button("add_list", addEntrySlot) {
                item = addEntryItem()
                onClick {
                    addListEntry(context.player, document, path, label, descriptor, resolvedDescriptor, schema)
                    context.refresh()
                }
            }
        }

        if (resolvedDescriptor.kind == StructureKind.MAP) {
            button("add_map", addEntrySlot) {
                item = addEntryItem()
                onClick {
                    addMapEntry(context.player, document, path, resolvedDescriptor, schema, label)
                    context.refresh()
                }
            }
        }

        button("search", if (showSaveButton) 48 else 47) {
            item = stackedItem(Material.COMPASS) {
                displayName = Component.text("Search")
                lore += Component.text("Search options in this menu")
            }.getItem()
            onClick {
                openSearchMenu(
                    context = context,
                    title = title,
                    document = document,
                    path = path,
                    label = label,
                    descriptor = descriptor,
                    schema = schema,
                    entries = entries,
                    onSave = onSave,
                    showSaveButton = showSaveButton
                )
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

        EditorCloseGuard.suppress(player)
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

    private suspend fun <T> openSearchMenu(
        context: EditorContext,
        title: Component,
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        label: String,
        descriptor: SerialDescriptor,
        schema: EditorSchema<T>?,
        entries: List<EditorEntry>,
        onSave: suspend () -> Unit,
        showSaveButton: Boolean
    ) {
        EditorCloseGuard.suppress(context.player)

        val resultSlots = (3..29).toList()
        val currentResults = MutableList<SearchResult?>(resultSlots.size) { null }
        var suppressReturnRefresh = false
        val searchIndex = buildGlobalSearchIndex(document, schema)

        suspend fun renderResults(inventory: PacketInventory, query: String) {
            val filtered = filterSearchEntries(searchIndex, query).take(resultSlots.size)

            filtered.forEachIndexed { index, entry ->
                currentResults[index] = entry
            }
            for (index in filtered.size until resultSlots.size) {
                currentResults[index] = null
            }

            inventory.setItem(1, stackedItem(Material.PAPER) {
                displayName = Component.text("Type to filter")
                lore += Component.text("Searches the editor index")
            }.getItem())
            inventory.setItem(2, stackedItem(Material.COMPASS) {
                displayName = Component.text("Matches: ${filtered.size}")
                lore += Component.text("Top ${filtered.size} result(s)")
            }.getItem())

            resultSlots.forEachIndexed { index, slot ->
                inventory.setItem(slot, currentResults[index]?.let(::searchResultIcon))
            }
            inventory.setItem(30, stackedItem(Material.BARRIER) {
                displayName = Component.text("Clear")
                lore += Component.text("Reset search query")
            }.getItem())
            inventory.setItem(31, stackedItem(Material.ARROW) {
                displayName = Component.text("Return")
                lore += Component.text("Back to editor")
            }.getItem())

            if (filtered.isEmpty()) {
                inventory.setItem(13, stackedItem(Material.GRAY_DYE) {
                    displayName = Component.text("No matching options")
                    lore += Component.text("Try a different query")
                }.getItem())
            }
        }

        context.player.createMenu(Component.text("Search"), InventoryType.ANVIL.onRename { _, name, inventory ->
            KMenu.scope.launch {
                val normalizedInput = normalizeSearchInput(name)
                if (normalizedInput != name) {
                    inventory.anvilInput = normalizedInput
                    inventory.setItem(0, searchInputItem(), update = false)
                }
                renderResults(inventory, parseSearchQuery(normalizedInput))
            }
        }) {
            menuFactory = { customTitle, customType, customPlayer, cancelInteractions ->
                object : PrivateMenu(customTitle, customType, customPlayer, cancelInteractions) {
                    override suspend fun onClosed(player: Player) {
                        if (!suppressReturnRefresh) {
                            context.refresh()
                        }
                    }
                }
            }

            button("search_input", 0) {
                item = searchInputItem()
            }

            resultSlots.forEachIndexed { index, slot ->
                button("search_result_$index", slot) {
                    item = stackedItem(Material.AIR) {}.getItem()
                    onClick { event ->
                        val entry = currentResults[index] ?: return@onClick
                        suppressReturnRefresh = true
                        handleEntryClick(
                            context = context,
                            title = title,
                            document = document,
                            entry = entry.entry,
                            schema = schema,
                            onSave = onSave,
                            buttonType = event.buttonType,
                            showSaveButton = showSaveButton
                        )
                    }
                }
            }

            button("search_clear", 30) {
                item = stackedItem(Material.BARRIER) {
                    displayName = Component.text("Clear")
                }.getItem()
                onClick {
                    val inventory = context.player.packetInventory() ?: return@onClick
                    inventory.anvilInput = SEARCH_PREFIX
                    inventory.setItem(0, searchInputItem())
                    renderResults(inventory, "")
                }
            }

            button("search_return", 31) {
                item = stackedItem(Material.ARROW) {
                    displayName = Component.text("Return")
                }.getItem()
                onClick {
                    withContext(BukkitCtx.ofEntity(context.player)) {
                        context.player.closeInventory()
                    }
                }
            }
        }.also {
            it.anvilInput = SEARCH_PREFIX
            it.setItem(0, searchInputItem(), update = false)
            renderResults(it, "")
        }.open(context.player)
    }

    private fun filterSearchEntries(entries: List<SearchResult>, query: String): List<SearchResult> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            return entries
        }

        val parts = normalized.split(' ').filter { it.isNotBlank() }
        return entries.filter { result -> parts.all(result.searchText::contains) }
    }

    private fun <T> buildGlobalSearchIndex(
        document: SerializableEditorDocument<T>,
        schema: EditorSchema<T>?
    ): List<SearchResult> {
        val results = linkedMapOf<String, SearchResult>()

        fun visit(path: List<PathSegment>, descriptor: SerialDescriptor, label: String) {
            val state = prepareNodeEditorState(document, path, descriptor, label, schema)
            for (entry in state.entries) {
                val meta = entry.meta
                if (meta?.searchable != false) {
                    val location = entry.path
                        .filterIsInstance<PathSegment.Key>()
                        .joinToString(" > ") { prettify(it.value) }
                        .ifBlank { "Root" }
                    val tags = buildSearchTags(entry)
                    val searchText = buildString {
                        append(entry.label.lowercase())
                        append(' ')
                        append(location.lowercase())
                        append(' ')
                        append(tags.joinToString(" ").lowercase())
                        if (meta?.description?.isNotEmpty() == true) {
                            append(' ')
                            append(meta.description.joinToString(" ").lowercase())
                        }
                    }
                    results.putIfAbsent(
                        "${entry.path.toSchemaPath()}::${entry.label}",
                        SearchResult(entry, location, tags, searchText)
                    )
                }

                if (entry.kind == NodeKind.OBJECT) {
                    visit(entry.path, entry.descriptor, entry.label)
                }
            }
        }

        visit(emptyList(), document.rootDescriptor(), "root")
        return results.values.toList()
    }

    private fun buildSearchTags(entry: EditorEntry): List<String> {
        val derived = buildSet {
            add(entry.label)
            entry.path.forEach { segment ->
                if (segment is PathSegment.Key) {
                    add(segment.value)
                    add(prettify(segment.value))
                }
            }
        }

        return (derived + entry.meta?.searchTags.orEmpty())
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun searchResultIcon(result: SearchResult) = stackedItem(
        result.entry.meta?.iconMaterial ?: when (result.entry.kind) {
            NodeKind.OBJECT -> objectMaterial(result.entry)
            NodeKind.LIST -> listMaterial(result.entry)
            NodeKind.MAP -> mapMaterial(result.entry)
            NodeKind.BOOLEAN -> Material.COMPARATOR
            NodeKind.STRING -> stringMaterial(result.entry)
            NodeKind.NUMBER -> numberMaterial(result.entry)
            NodeKind.ENUM -> enumMaterial(result.entry)
        }
    ) {
        displayName = EditorItemStyling.title(result.entry.label)
        lore += EditorItemStyling.valueLine("Location: ", result.location)
        if (result.tags.isNotEmpty()) {
            lore += EditorItemStyling.valueLine("Tags: ", result.tags.joinToString(", "))
        }
        if (result.entry.meta?.description?.isNotEmpty() == true) {
            lore += EditorItemStyling.section("Description")
            lore += result.entry.meta.description.map(EditorItemStyling::hint)
        }
    }.getItem()

    private fun searchInputItem() = stackedItem(Material.NAME_TAG) {
        displayName = Component.text(SEARCH_PREFIX)
    }.getItem()

    private fun normalizeSearchInput(input: String): String {
        return when {
            input.startsWith(SEARCH_PREFIX) -> input
            input.isBlank() -> SEARCH_PREFIX
            else -> SEARCH_PREFIX + input.removePrefix(SEARCH_PREFIX)
        }
    }

    private fun parseSearchQuery(input: String): String {
        return normalizeSearchInput(input).substringAfter(SEARCH_PREFIX).trim()
    }
}

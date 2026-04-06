package gg.aquatic.waves.serialization.editor

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalSerializationApi::class)
object SerializableEditor {

    private val defaultJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun <T> startEditing(
        player: Player,
        title: Component,
        serializer: KSerializer<T>,
        json: Json = defaultJson,
        schema: EditorSchema<T>? = null,
        loadFresh: () -> T,
        onSave: (T) -> Unit
    ) {
        val context = EditorContext(player)
        val document = SerializableEditorDocument(json, serializer, loadFresh())

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
                    runCatching {
                        onSave(document.decode())
                    }.onSuccess {
                        player.sendMessage("Changes saved successfully!")
                    }.onFailure {
                        player.sendMessage("Failed to save: ${it.message ?: it.javaClass.simpleName}")
                    }
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
        onSave: suspend () -> Unit
    ) {
        val editorState = prepareNodeEditorState(document, path, descriptor, label, schema)
        val menuTitle = if (path.isEmpty()) title else Component.text(label.take(32))
        val entrySlots = (0..44).toList()

        context.player.createMenu(menuTitle, InventoryType.GENERIC9X6) {
            editorState.entries.take(entrySlots.size).forEachIndexed { index, entry ->
                button("entry_${index}_${entry.label}", entrySlots[index]) {
                    item = entryIcon(entry)
                    onClick { event ->
                        handleEntryClick(context, title, document, entry, schema, onSave, event.buttonType)
                    }
                }
            }

            addEditorControls(context, document, path, label, descriptor, editorState.resolvedDescriptor, schema, onSave)
        }.open(context.player)
    }

    private fun <T> prepareNodeEditorState(
        document: SerializableEditorDocument<T>,
        path: List<PathSegment>,
        descriptor: SerialDescriptor,
        label: String,
        schema: EditorSchema<T>?
    ): NodeEditorState {
        val current = document.get(path)
        val currentContext = buildFieldContext(label, path, descriptor, current, document.root())
        val resolvedDescriptor = if (descriptor.kind is PolymorphicKind) {
            schema?.resolveDescriptor(currentContext) ?: descriptor
        } else {
            descriptor
        }
        val normalized = if (current == JsonNull && isContainer(resolvedDescriptor)) {
            defaultElement(document.json, resolvedDescriptor, useNullForNullable = false)
        } else {
            current
        }

        if (current == JsonNull && normalized != JsonNull) {
            document.set(path, normalized)
        }

        return NodeEditorState(
            resolvedDescriptor = resolvedDescriptor,
            entries = nodeEntries(path, resolvedDescriptor, normalized, document.root(), schema, document.json)
        )
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
                openNodeEditor(context, title, document, entry.path, entry.descriptor, entry.label, schema, onSave)
            }

            NodeKind.BOOLEAN -> {
                val currentValue = document.get(entry.path).jsonPrimitive.booleanOrNull ?: false
                document.set(entry.path, JsonPrimitive(!currentValue))
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
                document.set(entry.path, JsonNull)
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
        onSave: suspend () -> Unit
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
            defaultElement(document.json, resolvedDescriptor.getElementDescriptor(0), useNullForNullable = false)
        }
        if (created is JsonArray && resolvedDescriptor.getElementDescriptor(0).kind != StructureKind.LIST) {
            document.addAllToList(path, created)
        } else {
            document.addToList(path, created)
        }
    }

    private fun buildFieldContext(
        label: String,
        path: List<PathSegment>,
        descriptor: SerialDescriptor,
        value: JsonElement,
        root: JsonElement
    ) = EditorFieldContext(
        label = label,
        path = path.toSchemaPath(),
        pathSegments = path.toSchemaSegments(),
        description = emptyList(),
        descriptor = descriptor,
        value = value,
        root = root
    )

    private suspend fun <T> editPrimitiveValue(
        player: Player,
        document: SerializableEditorDocument<T>,
        entry: EditorEntry
    ) {
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }

        val prompt = buildPrompt(entry)
        EditorChatMessages.sendPrompt(
            player = player,
            prompt = prompt,
            allowNull = entry.descriptor.isNullable
        )
        val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return
        val parsed = parsePrimitive(input, entry.descriptor) ?: run {
            EditorChatMessages.sendError(player, "Invalid value.")
            return
        }

        document.set(entry.path, parsed)
    }

    private suspend fun <T> editEnumValue(
        player: Player,
        document: SerializableEditorDocument<T>,
        entry: EditorEntry
    ) {
        val selected = selectEnumValue(player, entry) ?: return
        document.set(entry.path, selected)
    }

    private suspend fun selectEnumValue(
        player: Player,
        entry: EditorEntry
    ): JsonElement? {
        val allowed = (0 until entry.descriptor.elementsCount)
            .map { entry.descriptor.getElementName(it) }
        val current = entry.element.jsonPrimitive.contentOrNull
        val entrySlots = (0..44).toList()
        val inventoryType = when {
            allowed.size <= 9 -> InventoryType.GENERIC9X3
            allowed.size <= 18 -> InventoryType.GENERIC9X4
            allowed.size <= 27 -> InventoryType.GENERIC9X5
            else -> InventoryType.GENERIC9X6
        }

        return withContext(BukkitCtx.ofEntity(player)) {
            suspendCancellableCoroutine { continuation ->
                KMenu.scope.launch {
                    player.createMenu(Component.text(entry.label.take(32)), inventoryType) {
                        allowed.take(entrySlots.size).forEachIndexed { index, value ->
                            button("enum_$index", entrySlots[index]) {
                                item = stackedItem(
                                    if (value == current) Material.LIME_DYE else Material.COMPARATOR
                                ) {
                                    displayName = Component.text(value)
                                    lore += Component.text(
                                        if (value == current) "Current value" else "Click to select"
                                    )
                                }.getItem()
                                onClick {
                                    continuation.resume(JsonPrimitive(value))
                                }
                            }
                        }

                        if (entry.descriptor.isNullable) {
                            button("enum_clear", 49) {
                                item = stackedItem(Material.BARRIER) {
                                    displayName = Component.text("Clear")
                                    lore += Component.text("Set this value to null")
                                }.getItem()
                                onClick {
                                    continuation.resume(JsonNull)
                                }
                            }
                        }

                        button("enum_cancel", if (inventoryType == InventoryType.GENERIC9X6) 53 else when (inventoryType) {
                            InventoryType.GENERIC9X5 -> 44
                            InventoryType.GENERIC9X4 -> 35
                            else -> 26
                        }) {
                            item = stackedItem(Material.ARROW) {
                                displayName = Component.text("Cancel")
                            }.getItem()
                            onClick {
                                continuation.resume(null)
                            }
                        }
                    }.open(player)
                }
            }
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
            value = defaultElement(document.json, descriptor.getElementDescriptor(1), useNullForNullable = false)
        )
    }

    private fun buildPrompt(entry: EditorEntry): String {
        entry.meta?.prompt?.let { return it }
        val nullableHint = if (entry.descriptor.isNullable) " Use 'null' to clear." else ""
        return when (entry.kind) {
            NodeKind.STRING -> "Enter value for ${entry.label}.$nullableHint"
            NodeKind.NUMBER -> "Enter numeric value for ${entry.label}.$nullableHint"
            NodeKind.ENUM -> {
                val allowed = (0 until entry.descriptor.elementsCount).joinToString(", ") {
                    entry.descriptor.getElementName(it)
                }
                "Enter one of [$allowed] for ${entry.label}.$nullableHint"
            }

            else -> "Enter value for ${entry.label}.$nullableHint"
        }
    }

    private fun parsePrimitive(raw: String, descriptor: SerialDescriptor): JsonElement? {
        if (descriptor.isNullable && raw.equals("null", ignoreCase = true)) {
            return JsonNull
        }

        return when {
            descriptor.kind == PrimitiveKind.STRING -> JsonPrimitive(raw)
            descriptor.kind == PrimitiveKind.BOOLEAN -> raw.toBooleanStrictOrNull()?.let(::JsonPrimitive)
            descriptor.kind == PrimitiveKind.INT -> raw.toIntOrNull()?.let(::JsonPrimitive)
            descriptor.kind == PrimitiveKind.LONG -> raw.toLongOrNull()?.let(::JsonPrimitive)
            descriptor.kind == PrimitiveKind.FLOAT -> raw.toFloatOrNull()?.let(::JsonPrimitive)
            descriptor.kind == PrimitiveKind.DOUBLE -> raw.toDoubleOrNull()?.let(::JsonPrimitive)
            descriptor.kind == PrimitiveKind.BYTE -> raw.toByteOrNull()?.let { JsonPrimitive(it.toInt()) }
            descriptor.kind == PrimitiveKind.SHORT -> raw.toShortOrNull()?.let { JsonPrimitive(it.toInt()) }
            descriptor.kind == SerialKind.ENUM -> {
                val match = (0 until descriptor.elementsCount)
                    .map { descriptor.getElementName(it) }
                    .firstOrNull { it.equals(raw, ignoreCase = true) }
                match?.let(::JsonPrimitive)
            }

            else -> null
        }
    }

    private fun <T> nodeEntries(
        basePath: List<PathSegment>,
        descriptor: SerialDescriptor,
        element: JsonElement,
        root: JsonElement,
        schema: EditorSchema<T>?,
        json: Json
    ): List<EditorEntry> {
        if (descriptor.kind is PolymorphicKind) {
            val resolved = schema?.resolveDescriptor(
                EditorFieldContext(
                    label = basePath.lastOrNull()?.let {
                        when (it) {
                            is PathSegment.Key -> it.value
                            is PathSegment.Index -> "#${it.value}"
                        }
                    } ?: "root",
                    path = basePath.toSchemaPath(),
                    pathSegments = basePath.toSchemaSegments(),
                    description = emptyList(),
                    descriptor = descriptor,
                    value = element,
                    root = root
                )
            ) ?: descriptor
            return nodeEntries(basePath, resolved, element, root, schema, json)
        }

        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                val obj = element as? JsonObject ?: JsonObject(emptyMap())
                (0 until descriptor.elementsCount).map { index ->
                    val name = descriptor.getElementName(index)
                    val childDescriptor = descriptor.getElementDescriptor(index)
                    val childElement = obj[name] ?: defaultElement(json, childDescriptor)
                    val absolutePath = basePath + PathSegment.Key(name)
                    val fieldContext = EditorFieldContext(
                        label = prettify(name),
                        path = absolutePath.toSchemaPath(),
                        pathSegments = absolutePath.toSchemaSegments(),
                        description = emptyList(),
                        descriptor = childDescriptor,
                        value = childElement,
                        root = root
                    )
                    val meta = schema?.resolve(fieldContext)
                    if (meta != null && !meta.visibleWhen(fieldContext)) {
                        return@map null
                    }
                    createEntry(
                        label = meta?.displayName ?: prettify(name),
                        descriptor = childDescriptor,
                        element = childElement,
                        path = absolutePath,
                        removable = false,
                        meta = meta
                    )
                }.filterNotNull()
            }

            StructureKind.LIST -> {
                val array = element as? JsonArray ?: JsonArray(emptyList())
                val childDescriptor = descriptor.getElementDescriptor(0)
                array.mapIndexed { index, child ->
                    val absolutePath = basePath + PathSegment.Index(index)
                    createEntry(
                        label = "#$index",
                        descriptor = childDescriptor,
                        element = child,
                        path = absolutePath,
                        removable = true,
                        meta = schema?.resolve(
                            EditorFieldContext(
                                label = "#$index",
                                path = absolutePath.toSchemaPath(),
                                pathSegments = absolutePath.toSchemaSegments(),
                                description = emptyList(),
                                descriptor = childDescriptor,
                                value = child,
                                root = root
                            )
                        )
                    )
                }
            }

            StructureKind.MAP -> {
                val obj = element as? JsonObject ?: JsonObject(emptyMap())
                val childDescriptor = descriptor.getElementDescriptor(1)
                obj.entries.sortedBy { it.key }.map { (key, child) ->
                    val absolutePath = basePath + PathSegment.Key(key)
                    createEntry(
                        label = key,
                        descriptor = childDescriptor,
                        element = child,
                        path = absolutePath,
                        removable = true,
                        meta = schema?.resolve(
                            EditorFieldContext(
                                label = key,
                                path = absolutePath.toSchemaPath(),
                                pathSegments = absolutePath.toSchemaSegments(),
                                description = emptyList(),
                                descriptor = childDescriptor,
                                value = child,
                                root = root
                            )
                        )
                    )
                }
            }

            else -> emptyList()
        }
    }

    private fun createEntry(
        label: String,
        descriptor: SerialDescriptor,
        element: JsonElement,
        path: List<PathSegment>,
        removable: Boolean,
        meta: FieldMeta?
    ): EditorEntry {
        return EditorEntry(
            label = label,
            descriptor = descriptor,
            element = element,
            path = path,
            removable = removable,
            kind = classify(descriptor),
            meta = meta
        )
    }

    private fun classify(descriptor: SerialDescriptor): NodeKind {
        return when (descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> NodeKind.OBJECT
            StructureKind.LIST -> NodeKind.LIST
            StructureKind.MAP -> NodeKind.MAP
            is PolymorphicKind -> NodeKind.OBJECT
            PrimitiveKind.BOOLEAN -> NodeKind.BOOLEAN
            PrimitiveKind.STRING -> NodeKind.STRING
            SerialKind.ENUM -> NodeKind.ENUM
            is PrimitiveKind -> NodeKind.NUMBER
            else -> NodeKind.STRING
        }
    }

    private fun entryIcon(entry: EditorEntry) = entry.meta?.adapter?.createItem(
        entry.toFieldContext(entry.rootElement),
    ) { defaultEntryIcon(entry) } ?: defaultEntryIcon(entry)

    private fun defaultEntryIcon(entry: EditorEntry) = when (entry.kind) {
        NodeKind.OBJECT -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: objectMaterial(entry)) {
            if (entry.element == JsonNull) {
                lore += EditorItemStyling.valueLine("State: ", "unset")
            }
            polymorphicType(entry.element)?.let { type ->
                lore += EditorItemStyling.valueLine("Type: ", prettify(type))
            }
            lore += EditorItemStyling.hint(if (entry.element == JsonNull) "Click to create" else "Open object")
            appendClearHint(entry)
        }

        NodeKind.LIST -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: listMaterial(entry)) {
            lore += EditorItemStyling.hint("Open list")
            lore += EditorItemStyling.valueLine("Summary: ", summary(entry.element))
            appendClearHint(entry)
        }

        NodeKind.MAP -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: mapMaterial(entry)) {
            lore += EditorItemStyling.hint("Open map")
            lore += EditorItemStyling.valueLine("Summary: ", summary(entry.element))
            appendClearHint(entry)
        }

        NodeKind.BOOLEAN -> buildEntryIcon(
            entry,
            entry.meta?.iconMaterial ?: if (entry.element.jsonPrimitive.booleanOrNull == true) Material.LIME_DYE else Material.GRAY_DYE
        ) {
            lore += EditorItemStyling.valueLine("Value: ", summary(entry.element))
            lore += EditorItemStyling.hint("Click to toggle")
        }

        NodeKind.STRING -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: stringMaterial(entry)) {
            val raw = summary(entry.element)
            lore += EditorItemStyling.valueLine("Value: ", raw)
            lore += EditorItemStyling.formattedPreview(raw)
        }

        NodeKind.NUMBER -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: numberMaterial(entry)) {
            lore += EditorItemStyling.valueLine("Value: ", summary(entry.element))
        }

        NodeKind.ENUM -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: enumMaterial(entry)) {
            lore += EditorItemStyling.valueLine("Value: ", summary(entry.element))
        }
    }

    private fun buildEntryIcon(
        entry: EditorEntry,
        material: Material,
        block: gg.aquatic.stacked.StackedItemBuilder.() -> Unit
    ) = stackedItem(material) {
        displayName = EditorItemStyling.title(entry.label)
        appendDescription(entry)
        block()
    }.getItem()

    private fun gg.aquatic.stacked.StackedItemBuilder.appendDescription(entry: EditorEntry) {
        if (entry.meta?.description?.isNotEmpty() == true) {
            lore += EditorItemStyling.section("Description")
            lore += entry.meta.description.map(EditorItemStyling::hint)
        }
    }

    private fun gg.aquatic.stacked.StackedItemBuilder.appendClearHint(entry: EditorEntry) {
        if (entry.descriptor.isNullable) {
            lore += EditorItemStyling.hint("Press Q to clear")
        }
    }

    private fun summary(element: JsonElement): String {
        return when (element) {
            JsonNull -> "null"
            is JsonObject -> "${element.size} entries"
            is JsonArray -> "${element.size} items"
            is JsonPrimitive -> element.contentOrNull ?: element.toString()
        }.take(80)
    }

    private fun isContainer(descriptor: SerialDescriptor): Boolean {
        return descriptor.kind == StructureKind.CLASS ||
            descriptor.kind == StructureKind.OBJECT ||
            descriptor.kind == StructureKind.LIST ||
            descriptor.kind == StructureKind.MAP ||
            descriptor.kind is PolymorphicKind
    }

    private fun defaultElement(json: Json, descriptor: SerialDescriptor, useNullForNullable: Boolean = true): JsonElement {
        if (descriptor.isNullable && useNullForNullable) return JsonNull

        return when {
            descriptor.kind is PolymorphicKind -> {
                JsonObject(emptyMap())
            }

            descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> JsonObject(
                buildMap {
                    repeat(descriptor.elementsCount) { index ->
                        put(
                            descriptor.getElementName(index),
                            defaultElement(json, descriptor.getElementDescriptor(index))
                        )
                    }
                }
            )

            descriptor.kind == StructureKind.LIST -> JsonArray(emptyList())
            descriptor.kind == StructureKind.MAP -> JsonObject(emptyMap())
            descriptor.kind == PrimitiveKind.STRING -> JsonPrimitive("")
            descriptor.kind == PrimitiveKind.BOOLEAN -> JsonPrimitive(false)
            descriptor.kind == PrimitiveKind.INT -> JsonPrimitive(0)
            descriptor.kind == PrimitiveKind.LONG -> JsonPrimitive(0L)
            descriptor.kind == PrimitiveKind.FLOAT -> JsonPrimitive(0f)
            descriptor.kind == PrimitiveKind.DOUBLE -> JsonPrimitive(0.0)
            descriptor.kind == PrimitiveKind.BYTE -> JsonPrimitive(0)
            descriptor.kind == PrimitiveKind.SHORT -> JsonPrimitive(0)
            descriptor.kind == SerialKind.ENUM -> JsonPrimitive(descriptor.getElementName(0))
            else -> JsonPrimitive("")
        }
    }

    private fun prettify(raw: String): String {
        return raw.replace('-', ' ').replace('_', ' ')
    }

    private fun objectMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "hologram" in key -> Material.END_CRYSTAL
            "preview" in key -> Material.ENDER_EYE
            "item" in key -> Material.ITEM_FRAME
            "reward" in key -> Material.CHEST
            "button" in key -> Material.STONE_BUTTON
            "condition" in key -> Material.TRIPWIRE_HOOK
            "action" in key -> Material.BLAZE_POWDER
            "price" in key -> Material.GOLD_INGOT
            "rarit" in key -> Material.NETHER_STAR
            else -> Material.BOOK
        }
    }

    private fun listMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "slot" in key -> Material.HOPPER
            "line" in key || "lore" in key -> Material.WRITABLE_BOOK
            "action" in key -> Material.BLAZE_POWDER
            "condition" in key -> Material.TRIPWIRE_HOOK
            "interactable" in key -> Material.ARMOR_STAND
            "price" in key -> Material.GOLD_INGOT
            else -> Material.BOOKSHELF
        }
    }

    private fun mapMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "reward" in key -> Material.CHEST_MINECART
            "rarit" in key -> Material.NETHER_STAR
            "button" in key -> Material.STONE_BUTTON
            else -> Material.LECTERN
        }
    }

    private fun stringMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "name" in key || "title" in key -> Material.NAME_TAG
            "material" in key -> Material.BRICKS
            "permission" in key -> Material.TRIPWIRE_HOOK
            "message" in key || "text" in key -> Material.PAPER
            "crate" in key || "id" in key -> Material.COMPASS
            "command" in key -> Material.COMMAND_BLOCK
            "sound" in key -> Material.NOTE_BLOCK
            else -> Material.PAPER
        }
    }

    private fun numberMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "slot" in key -> Material.HOPPER
            "chance" in key -> Material.EMERALD
            "distance" in key || "range" in key -> Material.SPYGLASS
            "amount" in key -> Material.COPPER_INGOT
            "scale" in key -> Material.AMETHYST_SHARD
            "duration" in key || "interpolation" in key -> Material.CLOCK
            else -> Material.GOLD_NUGGET
        }
    }

    private fun enumMaterial(entry: EditorEntry): Material {
        val key = entry.label.lowercase()
        return when {
            "billboard" in key || "transform" in key -> Material.ITEM_FRAME
            "face" in key -> Material.COMPASS
            "type" in key -> Material.COMPARATOR
            else -> Material.REPEATER
        }
    }

    private fun polymorphicType(element: JsonElement): String? {
        return (element as? JsonObject)
            ?.get("type")
            ?.let { it as? JsonPrimitive }
            ?.contentOrNull
    }

    private fun titleString(component: Component): String {
        return component.toString()
    }

    private data class EditorEntry(
        val label: String,
        val descriptor: SerialDescriptor,
        val element: JsonElement,
        val path: List<PathSegment>,
        val removable: Boolean,
        val kind: NodeKind,
        val meta: FieldMeta?,
    )

    private data class NodeEditorState(
        val resolvedDescriptor: SerialDescriptor,
        val entries: List<EditorEntry>,
    )

    private enum class NodeKind {
        OBJECT,
        LIST,
        MAP,
        STRING,
        BOOLEAN,
        NUMBER,
        ENUM
    }

    private sealed interface PathSegment {
        data class Key(val value: String) : PathSegment
        data class Index(val value: Int) : PathSegment
    }

    private class SerializableEditorDocument<T>(
        val json: Json,
        private val serializer: KSerializer<T>,
        value: T
    ) {
        private var root: JsonElement = json.encodeToJsonElement(serializer, value)

        fun root(): JsonElement = root

        fun replaceRoot(value: JsonElement) {
            root = value
        }

        fun decode(): T {
            return json.decodeFromJsonElement(serializer, root)
        }

        fun get(path: List<PathSegment>): JsonElement {
            var current = root
            for (segment in path) {
                current = when (segment) {
                    is PathSegment.Key -> (current as? JsonObject)?.get(segment.value) ?: JsonNull
                    is PathSegment.Index -> (current as? JsonArray)?.getOrNull(segment.value) ?: JsonNull
                }
            }
            return current
        }

        fun set(path: List<PathSegment>, value: JsonElement) {
            root = update(root, path, value)
        }

        fun remove(path: List<PathSegment>) {
            root = removeAt(root, path)
        }

        fun addToList(path: List<PathSegment>, value: JsonElement) {
            val current = get(path) as? JsonArray ?: JsonArray(emptyList())
            set(path, JsonArray(current + value))
        }

        fun addAllToList(path: List<PathSegment>, values: JsonArray) {
            val current = get(path) as? JsonArray ?: JsonArray(emptyList())
            set(path, JsonArray(current + values))
        }

        fun putToMap(path: List<PathSegment>, key: String, value: JsonElement) {
            val current = get(path) as? JsonObject ?: JsonObject(emptyMap())
            set(path, JsonObject(current.toMutableMap().apply { put(key, value) }))
        }

        private fun update(current: JsonElement, path: List<PathSegment>, value: JsonElement, depth: Int = 0): JsonElement {
            if (depth >= path.size) return value

            return when (val segment = path[depth]) {
                is PathSegment.Key -> {
                    val obj = current as? JsonObject ?: JsonObject(emptyMap())
                    val mutable = obj.toMutableMap()
                    mutable[segment.value] = update(mutable[segment.value] ?: JsonNull, path, value, depth + 1)
                    JsonObject(mutable)
                }

                is PathSegment.Index -> {
                    val arr = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                    while (arr.size <= segment.value) {
                        arr += JsonNull
                    }
                    arr[segment.value] = update(arr[segment.value], path, value, depth + 1)
                    JsonArray(arr)
                }
            }
        }

        private fun removeAt(current: JsonElement, path: List<PathSegment>, depth: Int = 0): JsonElement {
            if (depth >= path.size) return current

            return when (val segment = path[depth]) {
                is PathSegment.Key -> {
                    val obj = current as? JsonObject ?: return current
                    if (depth == path.lastIndex) {
                        JsonObject(obj.toMutableMap().apply { remove(segment.value) })
                    } else {
                        val mutable = obj.toMutableMap()
                        val existing = mutable[segment.value] ?: return current
                        mutable[segment.value] = removeAt(existing, path, depth + 1)
                        JsonObject(mutable)
                    }
                }

                is PathSegment.Index -> {
                    val arr = current as? JsonArray ?: return current
                    val mutable = arr.toMutableList()
                    if (segment.value !in mutable.indices) return current
                    if (depth == path.lastIndex) {
                        mutable.removeAt(segment.value)
                    } else {
                        mutable[segment.value] = removeAt(mutable[segment.value], path, depth + 1)
                    }
                    JsonArray(mutable)
                }
            }
        }
    }

    private val EditorEntry.rootElement: JsonElement
        get() = element

    private fun EditorEntry.toFieldContext(root: JsonElement): EditorFieldContext {
        return EditorFieldContext(
            label = label,
            path = path.toSchemaPath(),
            pathSegments = path.toSchemaSegments(),
            description = meta?.description.orEmpty(),
            descriptor = descriptor,
            value = element,
            root = root
        )
    }

    private fun List<PathSegment>.toSchemaSegments(): List<String> {
        return map {
            when (it) {
                is PathSegment.Key -> it.value
                is PathSegment.Index -> it.value.toString()
            }
        }
    }

    private fun List<PathSegment>.toSchemaPath(): String {
        return toSchemaSegments().joinToString(".")
    }
}

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package gg.aquatic.waves.serialization.editor

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import gg.aquatic.stacked.StackedItemBuilder
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.EditorItemStyling
import gg.aquatic.waves.serialization.editor.meta.EditorSchema
import gg.aquatic.waves.serialization.editor.meta.FieldMeta
import gg.aquatic.waves.serialization.editor.meta.booleanOrNull
import gg.aquatic.waves.serialization.editor.meta.defaultYamlElement
import gg.aquatic.waves.serialization.editor.meta.displayString
import gg.aquatic.waves.serialization.editor.meta.stringContentOrNull
import gg.aquatic.waves.serialization.editor.meta.yamlList
import gg.aquatic.waves.serialization.editor.meta.yamlMap
import org.bukkit.Material
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

internal fun <T> prepareNodeEditorState(
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
    val normalized = if (current is YamlNull && isContainer(resolvedDescriptor)) {
        defaultYamlElement(resolvedDescriptor, useNullForNullable = false)
    } else {
        current
    }

    if (current is YamlNull && normalized !is YamlNull) {
        document.set(path, normalized)
    }

    return NodeEditorState(
        resolvedDescriptor = resolvedDescriptor,
        entries = nodeEntries(path, resolvedDescriptor, normalized, document.root(), schema)
    )
}

internal fun passthroughEntry(
    path: List<PathSegment>,
    descriptor: SerialDescriptor,
    entries: List<EditorEntry>
): EditorEntry? {
    if (descriptor.kind != StructureKind.CLASS && descriptor.kind != StructureKind.OBJECT) {
        return null
    }

    val onlyEntry = entries.singleOrNull() ?: return null
    if (onlyEntry.kind !in setOf(NodeKind.OBJECT, NodeKind.LIST, NodeKind.MAP)) {
        return null
    }

    val adapter = onlyEntry.meta?.adapter
    if (adapter != null && adapter !== gg.aquatic.waves.serialization.editor.meta.DefaultEditorFieldAdapter) {
        return null
    }

    if (onlyEntry.removable) {
        return null
    }

    if (onlyEntry.path == path) {
        return null
    }

    return onlyEntry
}

internal fun buildFieldContext(
    label: String,
    path: List<PathSegment>,
    descriptor: SerialDescriptor,
    value: YamlNode,
    root: YamlNode
) = EditorFieldContext(
    label = label,
    path = path.toSchemaPath(),
    pathSegments = path.toSchemaSegments(),
    description = emptyList(),
    descriptor = descriptor,
    value = value,
    root = root
)

internal fun <T> nodeEntries(
    basePath: List<PathSegment>,
    descriptor: SerialDescriptor,
    element: YamlNode,
    root: YamlNode,
    schema: EditorSchema<T>?
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
        return nodeEntries(basePath, resolved, element, root, schema)
    }

    return when (descriptor.kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT -> {
            val obj = element as? YamlMap ?: yamlMap(emptyMap())
            (0 until descriptor.elementsCount).map { index ->
                val name = descriptor.getElementName(index)
                val childDescriptor = descriptor.getElementDescriptor(index)
                val childElement = obj.get<YamlNode>(name) ?: defaultYamlElement(childDescriptor)
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
            val array = element as? YamlList ?: yamlList(emptyList())
            val childDescriptor = descriptor.getElementDescriptor(0)
            array.items.mapIndexed { index, child ->
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
            val obj = element as? YamlMap ?: yamlMap(emptyMap())
            val childDescriptor = descriptor.getElementDescriptor(1)
            obj.entries.entries.sortedBy { it.key.content }.map { (key, child) ->
                val absolutePath = basePath + PathSegment.Key(key.content)
                createEntry(
                    label = key.content,
                    descriptor = childDescriptor,
                    element = child,
                    path = absolutePath,
                    removable = true,
                    meta = schema?.resolve(
                        EditorFieldContext(
                            label = key.content,
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

internal fun createEntry(
    label: String,
    descriptor: SerialDescriptor,
    element: YamlNode,
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

internal fun classify(descriptor: SerialDescriptor): NodeKind {
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

internal fun entryIcon(entry: EditorEntry) = entry.meta?.adapter?.createItem(
    entry.toFieldContext(entry.rootElement),
) { defaultEntryIcon(entry) } ?: defaultEntryIcon(entry)

internal fun defaultEntryIcon(entry: EditorEntry) = when (entry.kind) {
    NodeKind.OBJECT -> buildEntryIcon(entry, entry.meta?.iconMaterial ?: objectMaterial(entry)) {
        if (entry.element is YamlNull) {
            lore += EditorItemStyling.valueLine("State: ", "unset")
        }
        polymorphicType(entry.element)?.let { type ->
            lore += EditorItemStyling.valueLine("Type: ", prettify(type))
        }
        lore += EditorItemStyling.hint(if (entry.element is YamlNull) "Click to create" else "Open object")
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
        entry.meta?.iconMaterial ?: if (entry.element.booleanOrNull == true) Material.LIME_DYE else Material.GRAY_DYE
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

internal fun buildEntryIcon(
    entry: EditorEntry,
    material: Material,
    block: StackedItemBuilder.() -> Unit
) = stackedItem(material) {
    displayName = EditorItemStyling.title(entry.label)
    appendDescription(entry)
    block()
}.getItem()

internal fun StackedItemBuilder.appendDescription(entry: EditorEntry) {
    if (entry.meta?.description?.isNotEmpty() == true) {
        lore += EditorItemStyling.section("Description")
        lore += entry.meta.description.map(EditorItemStyling::hint)
    }
}

internal fun StackedItemBuilder.appendClearHint(entry: EditorEntry) {
    if (entry.descriptor.isNullable) {
        lore += EditorItemStyling.hint("Press Q to clear")
    }
}

internal fun summary(element: YamlNode): String {
    return element.displayString().take(80)
}

internal fun isContainer(descriptor: SerialDescriptor): Boolean {
    return descriptor.kind == StructureKind.CLASS ||
        descriptor.kind == StructureKind.OBJECT ||
        descriptor.kind == StructureKind.LIST ||
        descriptor.kind == StructureKind.MAP ||
        descriptor.kind is PolymorphicKind
}

internal fun prettify(raw: String): String {
    return raw.replace('-', ' ').replace('_', ' ')
}

internal fun objectMaterial(entry: EditorEntry): Material {
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

internal fun listMaterial(entry: EditorEntry): Material {
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

internal fun mapMaterial(entry: EditorEntry): Material {
    val key = entry.label.lowercase()
    return when {
        "reward" in key -> Material.CHEST_MINECART
        "rarit" in key -> Material.NETHER_STAR
        "button" in key -> Material.STONE_BUTTON
        else -> Material.LECTERN
    }
}

internal fun stringMaterial(entry: EditorEntry): Material {
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

internal fun numberMaterial(entry: EditorEntry): Material {
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

internal fun enumMaterial(entry: EditorEntry): Material {
    val key = entry.label.lowercase()
    return when {
        "billboard" in key || "transform" in key -> Material.ITEM_FRAME
        "face" in key -> Material.COMPASS
        "type" in key -> Material.COMPARATOR
        else -> Material.REPEATER
    }
}

internal fun polymorphicType(element: YamlNode): String? {
    return (element as? YamlMap)
        ?.get<YamlNode>("type")
        ?.stringContentOrNull
}

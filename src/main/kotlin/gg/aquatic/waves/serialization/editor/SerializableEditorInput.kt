package gg.aquatic.waves.serialization.editor

import com.charleskorn.kaml.YamlNode
import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.serialization.editor.meta.EditorChatMessages
import gg.aquatic.waves.serialization.editor.meta.EditorSelectionMenus
import gg.aquatic.waves.serialization.editor.meta.NumberFieldSupport
import gg.aquatic.waves.serialization.editor.meta.stringContentOrNull
import gg.aquatic.waves.serialization.editor.meta.yamlNull
import gg.aquatic.waves.serialization.editor.meta.yamlScalar
import kotlinx.coroutines.withContext
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import org.bukkit.entity.Player

internal suspend fun <T> editPrimitiveValue(
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

internal suspend fun <T> editEnumValue(
    player: Player,
    document: SerializableEditorDocument<T>,
    entry: EditorEntry
) {
    val selected = selectEnumValue(player, entry) ?: return
    document.set(entry.path, selected)
}

internal suspend fun selectEnumValue(
    player: Player,
    entry: EditorEntry
): YamlNode? {
    val allowed = (0 until entry.descriptor.elementsCount)
        .map { entry.descriptor.getElementName(it) }
    val current = entry.element.stringContentOrNull
    val selected = withContext(BukkitCtx.ofEntity(player)) {
        EditorSelectionMenus.selectScalarOption(
            player = player,
            title = entry.label,
            allowed = allowed,
            current = current,
            nullable = entry.descriptor.isNullable
        )
    }

    return when {
        selected == null && entry.descriptor.isNullable -> yamlNull()
        selected == null || selected == current -> null
        else -> yamlScalar(selected)
    }
}

internal fun buildPrompt(entry: EditorEntry): String {
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

internal fun parsePrimitive(raw: String, descriptor: SerialDescriptor): YamlNode? {
    if (descriptor.isNullable && raw.equals("null", ignoreCase = true)) {
        return yamlNull()
    }

    return when {
        descriptor.kind == PrimitiveKind.STRING -> yamlScalar(raw)
        descriptor.kind == PrimitiveKind.BOOLEAN -> raw.toBooleanStrictOrNull()?.let(::yamlScalar)
        descriptor.kind == PrimitiveKind.INT -> NumberFieldSupport.parseNode(raw, "Invalid integer.", String::toIntOrNull)
        descriptor.kind == PrimitiveKind.LONG -> NumberFieldSupport.parseNode(raw, "Invalid long.", String::toLongOrNull)
        descriptor.kind == PrimitiveKind.FLOAT -> NumberFieldSupport.parseNode(raw, "Invalid float.", String::toFloatOrNull)
        descriptor.kind == PrimitiveKind.DOUBLE -> NumberFieldSupport.parseNode(raw, "Invalid number.", String::toDoubleOrNull)
        descriptor.kind == PrimitiveKind.BYTE -> NumberFieldSupport.parseNode(raw, "Invalid byte.", String::toByteOrNull)
        descriptor.kind == PrimitiveKind.SHORT -> NumberFieldSupport.parseNode(raw, "Invalid short.", String::toShortOrNull)
        descriptor.kind == SerialKind.ENUM -> {
            val match = (0 until descriptor.elementsCount)
                .map { descriptor.getElementName(it) }
                .firstOrNull { it.equals(raw, ignoreCase = true) }
            match?.let(::yamlScalar)
        }

        else -> null
    }
}

package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext

object EditorEntryFactories {

    fun text(
        prompt: String,
        validator: suspend (String) -> String? = { null },
        transform: suspend (String) -> YamlNode = { yamlScalar(it) }
    ): EntryFactory {
        return EntryFactory { player, _ ->
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
            EditorChatMessages.sendPrompt(player, prompt)
            val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return@EntryFactory null
            val error = validator(input)
            if (error != null) {
                EditorChatMessages.sendError(player, error)
                return@EntryFactory null
            }
            transform(input)
        }
    }

    fun int(
        prompt: String,
        min: Int? = null,
        max: Int? = null,
        unique: Boolean = false,
    ): EntryFactory = EntryFactory { player, context ->
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }
        EditorChatMessages.sendPrompt(
            player,
            prompt,
            extraHints = listOf("You can use ranges like '0-8' or lists like '0,2,4'.")
        )
        val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return@EntryFactory null
        val error = validateIntegerBatch(input, min, max)
        if (error != null) {
            EditorChatMessages.sendError(player, error)
            return@EntryFactory null
        }

        val values = parseIntegerBatch(input)
        val filteredValues = if (unique) {
            val existing = (context.value as? com.charleskorn.kaml.YamlList)
                ?.items
                ?.mapNotNull { element -> element.intOrNull }
                ?.toHashSet()
                ?: hashSetOf()
            values.filter { value -> existing.add(value) }
        } else {
            values
        }

        when (filteredValues.size) {
            0 -> null
            1 -> yamlScalar(filteredValues.single().toString())
            else -> yamlList(filteredValues.map { yamlScalar(it.toString()) })
        }
    }

    fun float(
        prompt: String,
        min: Float? = null,
        max: Float? = null,
    ): EntryFactory = text(
        prompt = prompt,
        validator = { raw ->
            val parsed = raw.toFloatOrNull() ?: return@text "Invalid float."
            if (min != null && parsed < min) return@text "Value must be at least $min."
            if (max != null && parsed > max) return@text "Value must be at most $max."
            null
        },
        transform = { yamlScalar(it.toFloat().toString()) }
    )

    fun boolean(prompt: String): EntryFactory = text(
        prompt = prompt,
        validator = {
            if (it.equals("true", true) || it.equals("false", true)) null else "Invalid boolean."
        },
        transform = { yamlScalar(it.toBooleanStrict().toString()) }
    )

    fun map(
        keyPrompt: String,
        valueFactory: suspend (String) -> YamlNode,
        keyValidator: suspend (String) -> String? = { null }
    ): MapEntryFactory {
        return MapEntryFactory { player, _ ->
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
            EditorChatMessages.sendPrompt(player, keyPrompt)
            val key = ChatInput.createHandle(listOf("cancel")).await(player)?.trim().orEmpty()
            if (key.isEmpty()) return@MapEntryFactory null
            val error = keyValidator(key)
            if (error != null) {
                EditorChatMessages.sendError(player, error)
                return@MapEntryFactory null
            }
            key to valueFactory(key)
        }
    }

    private fun validateIntegerBatch(raw: String, min: Int?, max: Int?): String? {
        val values = runCatching { parseIntegerBatch(raw) }.getOrElse { return it.message ?: "Invalid integer range." }
        values.forEach { parsed ->
            if (min != null && parsed < min) return "Value must be at least $min."
            if (max != null && parsed > max) return "Value must be at most $max."
        }
        return null
    }

    private fun parseIntegerBatch(raw: String): List<Int> {
        val input = raw.trim()
        if (input.isEmpty()) {
            throw IllegalArgumentException("Invalid integer.")
        }

        return input.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { token ->
                val rangeSeparator = token.indexOf('-')
                if (rangeSeparator <= 0 || rangeSeparator == token.lastIndex) {
                    val single = token.toIntOrNull() ?: throw IllegalArgumentException("Invalid integer.")
                    return@flatMap listOf(single)
                }

                val start = token.substring(0, rangeSeparator).trim().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid integer range.")
                val end = token.substring(rangeSeparator + 1).trim().toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid integer range.")
                if (end < start) {
                    throw IllegalArgumentException("Range end must be at least range start.")
                }
                (start..end).toList()
            }
    }
}

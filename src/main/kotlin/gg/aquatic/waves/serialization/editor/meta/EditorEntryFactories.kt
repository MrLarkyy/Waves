package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

object EditorEntryFactories {

    fun text(
        prompt: String,
        validator: suspend (String) -> String? = { null },
        transform: suspend (String) -> JsonElement = { JsonPrimitive(it) }
    ): EntryFactory {
        return EntryFactory { player, _ ->
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
            player.sendMessage(prompt)
            val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return@EntryFactory null
            val error = validator(input)
            if (error != null) {
                player.sendMessage(error)
                return@EntryFactory null
            }
            transform(input)
        }
    }

    fun int(
        prompt: String,
        min: Int? = null,
        max: Int? = null,
    ): EntryFactory = text(
        prompt = prompt,
        validator = { raw ->
            val parsed = raw.toIntOrNull() ?: return@text "Invalid integer."
            if (min != null && parsed < min) return@text "Value must be at least $min."
            if (max != null && parsed > max) return@text "Value must be at most $max."
            null
        },
        transform = { JsonPrimitive(it.toInt()) }
    )

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
        transform = { JsonPrimitive(it.toFloat()) }
    )

    fun boolean(prompt: String): EntryFactory = text(
        prompt = prompt,
        validator = {
            if (it.equals("true", true) || it.equals("false", true)) null else "Invalid boolean."
        },
        transform = { JsonPrimitive(it.toBooleanStrict()) }
    )

    fun map(
        keyPrompt: String,
        valueFactory: suspend (String) -> JsonElement,
        keyValidator: suspend (String) -> String? = { null }
    ): MapEntryFactory {
        return MapEntryFactory { player, _ ->
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
            player.sendMessage(keyPrompt)
            val key = ChatInput.createHandle(listOf("cancel")).await(player)?.trim().orEmpty()
            if (key.isEmpty()) return@MapEntryFactory null
            val error = keyValidator(key)
            if (error != null) {
                player.sendMessage(error)
                return@MapEntryFactory null
            }
            key to valueFactory(key)
        }
    }
}

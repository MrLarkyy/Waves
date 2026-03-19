package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.stacked.StackedItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material
import java.util.Locale

data class MaterialLikeFieldConfig(
    override val prompt: String = "Enter material or Factory:ItemId:",
    override val iconMaterial: Material = Material.CHEST,
) : BaseTextInputFieldAdapter.Config

object MaterialLikeFieldAdapter : BaseTextInputFieldAdapter<MaterialLikeFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: MaterialLikeFieldConfig): Result<JsonElement> {
        val normalized = validateMaterialLike(raw)
            ?: return Result.failure(IllegalArgumentException("Invalid material or factory item id."))
        return Result.success(JsonPrimitive(normalized))
    }

    private fun validateMaterialLike(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        val material = Material.matchMaterial(value)
            ?: Material.matchMaterial(value.uppercase(Locale.ROOT))
        if (material != null) return material.name

        val splitIdx = value.indexOf(':')
        if (splitIdx <= 0 || splitIdx >= value.lastIndex) return null

        val factory = value.substring(0, splitIdx).trim()
        val itemId = value.substring(splitIdx + 1).trim()
        if (factory.isEmpty() || itemId.isEmpty()) return null

        val candidates = linkedSetOf(factory, factory.lowercase(Locale.ROOT), factory.uppercase(Locale.ROOT))
        val exists = candidates.any { candidate ->
            StackedItem.ITEM_FACTORIES[candidate]?.create(itemId) != null
        }

        return if (exists) "$factory:$itemId" else null
    }
}

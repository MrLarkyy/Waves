package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

data class IntFieldConfig(
    override val prompt: String,
    val min: Int? = null,
    val max: Int? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object IntFieldAdapter : BaseTextInputFieldAdapter<IntFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: IntFieldConfig): Result<JsonElement> {
        val parsed = raw.toIntOrNull() ?: return Result.failure(IllegalArgumentException("Invalid integer."))
        if (config.min != null && parsed < config.min) return Result.failure(IllegalArgumentException("Value must be at least ${config.min}."))
        if (config.max != null && parsed > config.max) return Result.failure(IllegalArgumentException("Value must be at most ${config.max}."))
        return Result.success(JsonPrimitive(parsed))
    }
}

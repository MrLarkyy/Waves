package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

data class DoubleFieldConfig(
    override val prompt: String,
    val min: Double? = null,
    val max: Double? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object DoubleFieldAdapter : BaseTextInputFieldAdapter<DoubleFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: DoubleFieldConfig): Result<JsonElement> {
        val parsed = raw.toDoubleOrNull() ?: return Result.failure(IllegalArgumentException("Invalid number."))
        if (config.min != null && parsed < config.min) return Result.failure(IllegalArgumentException("Value must be at least ${config.min}."))
        if (config.max != null && parsed > config.max) return Result.failure(IllegalArgumentException("Value must be at most ${config.max}."))
        return Result.success(JsonPrimitive(parsed))
    }
}

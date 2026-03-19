package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

data class EnumFieldConfig(
    override val prompt: String,
    val values: () -> Collection<String>,
    override val iconMaterial: Material = Material.COMPARATOR,
) : BaseTextInputFieldAdapter.Config

object EnumFieldAdapter : BaseTextInputFieldAdapter<EnumFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: EnumFieldConfig): Result<JsonElement> {
        val allowed = config.values()
        val match = allowed.firstOrNull { it.equals(raw, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Allowed: ${allowed.joinToString(", ")}"))
        return Result.success(JsonPrimitive(match))
    }
}

package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

data class TextFieldConfig(
    override val prompt: String,
    override val iconMaterial: Material = Material.PAPER,
    val validator: suspend (String) -> String? = { null },
    override val showFormattedPreview: Boolean = false,
) : BaseTextInputFieldAdapter.Config

object TextFieldAdapter : BaseTextInputFieldAdapter<TextFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: TextFieldConfig): Result<JsonElement> {
        return config.validator(raw)?.let { Result.failure(IllegalArgumentException(it)) }
            ?: Result.success(JsonPrimitive(raw))
    }
}

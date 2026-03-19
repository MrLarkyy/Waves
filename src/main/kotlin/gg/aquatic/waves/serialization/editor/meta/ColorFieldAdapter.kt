package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Color
import org.bukkit.Material

data class ColorFieldConfig(
    override val prompt: String = "Enter color (#RRGGBB or r;g;b):",
    override val iconMaterial: Material = Material.LEATHER_CHESTPLATE,
) : BaseTextInputFieldAdapter.Config

object ColorFieldAdapter : BaseTextInputFieldAdapter<ColorFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: ColorFieldConfig): Result<JsonElement> {
        return if (parseColor(raw) == null) {
            Result.failure(IllegalArgumentException("Invalid color."))
        } else {
            Result.success(JsonPrimitive(raw.trim()))
        }
    }

    private fun parseColor(raw: String): Color? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        if (value.startsWith("#") && value.length == 7) {
            return runCatching {
                Color.fromRGB(
                    value.substring(1, 3).toInt(16),
                    value.substring(3, 5).toInt(16),
                    value.substring(5, 7).toInt(16)
                )
            }.getOrNull()
        }

        val split = if (';' in value) value.split(";") else value.split(",")
        if (split.size != 3) return null

        val red = split[0].trim().toIntOrNull() ?: return null
        val green = split[1].trim().toIntOrNull() ?: return null
        val blue = split[2].trim().toIntOrNull() ?: return null
        return runCatching { Color.fromRGB(red, green, blue) }.getOrNull()
    }
}

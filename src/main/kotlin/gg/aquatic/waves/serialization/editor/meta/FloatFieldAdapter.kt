package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material

data class FloatFieldConfig(
    override val prompt: String,
    val min: Float? = null,
    val max: Float? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object FloatFieldAdapter : BaseTextInputFieldAdapter<FloatFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: FloatFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid float.",
            min = config.min,
            max = config.max,
            parse = String::toFloatOrNull
        )
    }
}

package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material

data class DoubleFieldConfig(
    override val prompt: String,
    val min: Double? = null,
    val max: Double? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object DoubleFieldAdapter : BaseTextInputFieldAdapter<DoubleFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: DoubleFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid number.",
            min = config.min,
            max = config.max,
            parse = String::toDoubleOrNull
        )
    }
}

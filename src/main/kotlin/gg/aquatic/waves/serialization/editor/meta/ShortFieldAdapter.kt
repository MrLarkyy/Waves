package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material

data class ShortFieldConfig(
    override val prompt: String,
    val min: Short? = null,
    val max: Short? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object ShortFieldAdapter : BaseTextInputFieldAdapter<ShortFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: ShortFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid short.",
            min = config.min,
            max = config.max,
            parse = String::toShortOrNull
        )
    }
}

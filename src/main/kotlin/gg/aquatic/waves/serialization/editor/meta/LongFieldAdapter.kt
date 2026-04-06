package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material

data class LongFieldConfig(
    override val prompt: String,
    val min: Long? = null,
    val max: Long? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object LongFieldAdapter : BaseTextInputFieldAdapter<LongFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: LongFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid long.",
            min = config.min,
            max = config.max,
            parse = String::toLongOrNull
        )
    }
}

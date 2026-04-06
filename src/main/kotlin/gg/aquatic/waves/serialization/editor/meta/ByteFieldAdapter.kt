package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material

data class ByteFieldConfig(
    override val prompt: String,
    val min: Byte? = null,
    val max: Byte? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object ByteFieldAdapter : BaseTextInputFieldAdapter<ByteFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: ByteFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid byte.",
            min = config.min,
            max = config.max,
            parse = String::toByteOrNull
        )
    }
}

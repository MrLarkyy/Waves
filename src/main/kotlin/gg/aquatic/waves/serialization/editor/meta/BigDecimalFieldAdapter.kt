package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material
import java.math.BigDecimal

data class BigDecimalFieldConfig(
    override val prompt: String,
    val min: BigDecimal? = null,
    val max: BigDecimal? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object BigDecimalFieldAdapter : BaseTextInputFieldAdapter<BigDecimalFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: BigDecimalFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid decimal number.",
            min = config.min,
            max = config.max,
            parse = { value -> value.toBigDecimalOrNull() },
            render = BigDecimal::toPlainString
        )
    }
}

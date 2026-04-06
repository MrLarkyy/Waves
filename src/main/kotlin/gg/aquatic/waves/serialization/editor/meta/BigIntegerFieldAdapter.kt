package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material
import java.math.BigInteger

data class BigIntegerFieldConfig(
    override val prompt: String,
    val min: BigInteger? = null,
    val max: BigInteger? = null,
    override val iconMaterial: Material = Material.GOLD_NUGGET,
) : BaseTextInputFieldAdapter.Config

object BigIntegerFieldAdapter : BaseTextInputFieldAdapter<BigIntegerFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: BigIntegerFieldConfig): Result<YamlNode> {
        return NumberFieldSupport.parse(
            raw = raw,
            invalidMessage = "Invalid big integer.",
            min = config.min,
            max = config.max,
            parse = { value -> value.toBigIntegerOrNull() }
        )
    }
}

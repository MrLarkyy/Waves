package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import org.bukkit.Material
import java.util.*

data class MaterialFieldConfig(
    override val prompt: String = "Enter material:",
    override val iconMaterial: Material = Material.CHEST,
) : BaseTextInputFieldAdapter.Config

object MaterialFieldAdapter : BaseTextInputFieldAdapter<MaterialFieldConfig>() {
    override suspend fun parse(raw: String, context: EditorFieldContext, config: MaterialFieldConfig): Result<YamlNode> {
        val material = Material.matchMaterial(raw.trim())
            ?: Material.matchMaterial(raw.trim().uppercase(Locale.ROOT))
            ?: return Result.failure(IllegalArgumentException("Invalid material."))
        return Result.success(yamlScalar(material.name))
    }
}

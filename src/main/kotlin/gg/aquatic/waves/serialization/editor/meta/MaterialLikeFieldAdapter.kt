package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import gg.aquatic.stacked.ItemEncoder
import gg.aquatic.stacked.StackedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder

data class MaterialLikeFieldConfig(
    override val prompt: String = "Enter material or Factory:ItemId:",
    override val iconMaterial: Material = Material.CHEST,
    val allowHandShortcut: Boolean = false,
) : BaseTextInputFieldAdapter.Config

object MaterialLikeFieldAdapter : BaseTextInputFieldAdapter<MaterialLikeFieldConfig>() {
    override suspend fun prepareInput(
        player: Player,
        raw: String,
        context: EditorFieldContext,
        config: MaterialLikeFieldConfig
    ): Result<String> {
        if (!config.allowHandShortcut || !raw.equals("hand", ignoreCase = true)) {
            return Result.success(raw)
        }

        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.isEmpty) {
            return Result.failure(IllegalArgumentException("Your main hand is empty."))
        }

        val encoded = Base64Coder.encodeLines(ItemEncoder.encode(itemInHand.ensureSingleItem()))
            .replace("\r", "")
            .replace("\n", "")
        return Result.success("base64:$encoded")
    }

    override suspend fun parse(raw: String, context: EditorFieldContext, config: MaterialLikeFieldConfig): Result<YamlNode> {
        val normalized = validateMaterialLike(raw)
            ?: return Result.failure(IllegalArgumentException("Invalid material or factory item id."))
        return Result.success(yamlScalar(normalized))
    }

    private fun ItemStack.ensureSingleItem(): ItemStack = clone().apply { amount = 1 }

    private fun validateMaterialLike(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        val material = Material.matchMaterial(value)
            ?: Material.matchMaterial(value.uppercase(Locale.ROOT))
        if (material != null) return material.name

        val splitIdx = value.indexOf(':')
        if (splitIdx <= 0 || splitIdx >= value.lastIndex) return null

        val factory = value.substring(0, splitIdx).trim()
        val itemId = value.substring(splitIdx + 1).trim()
        if (factory.isEmpty() || itemId.isEmpty()) return null

        val candidates = linkedSetOf(factory, factory.lowercase(Locale.ROOT), factory.uppercase(Locale.ROOT))
        val exists = candidates.any { candidate ->
            StackedItem.ITEM_FACTORIES[candidate]?.create(itemId) != null
        }

        return if (exists) "$factory:$itemId" else null
    }
}

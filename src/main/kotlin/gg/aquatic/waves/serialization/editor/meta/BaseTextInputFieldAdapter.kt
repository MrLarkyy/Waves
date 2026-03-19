package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class BaseTextInputFieldAdapter<C : BaseTextInputFieldAdapter.Config> : ConfigurableFieldAdapter<C> {

    interface Config {
        val prompt: String
        val iconMaterial: Material
        val showFormattedPreview: Boolean
            get() = false
    }

    final override fun createItem(context: EditorFieldContext, config: C, defaultItem: () -> ItemStack): ItemStack {
        val rawValue = context.value.toString().trim('"')
        return stackedItem(config.iconMaterial) {
            displayName = EditorItemStyling.title(context.label)
            if (context.description.isNotEmpty()) {
                lore += EditorItemStyling.section("Description")
                lore += context.description.map(EditorItemStyling::hint)
            }
            lore += EditorItemStyling.valueLine("Value: ", EditorItemStyling.rawValue(rawValue))
            lore += EditorItemStyling.formattedPreview(rawValue, force = config.showFormattedPreview)
        }.getItem()
    }

    final override suspend fun edit(player: Player, context: EditorFieldContext, config: C): FieldEditResult {
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }
        player.sendMessage(config.prompt)
        val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return FieldEditResult.NoChange
        return parse(input.trim(), context, config).fold(
            onSuccess = { FieldEditResult.Updated(it) },
            onFailure = {
                player.sendMessage(it.message ?: "Invalid value.")
                FieldEditResult.NoChange
            }
        )
    }

    protected abstract suspend fun parse(raw: String, context: EditorFieldContext, config: C): Result<JsonElement>
}

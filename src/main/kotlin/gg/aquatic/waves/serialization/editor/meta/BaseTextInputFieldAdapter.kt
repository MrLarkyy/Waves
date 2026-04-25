package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.serialization.editor.EditorCloseGuard
import gg.aquatic.waves.serialization.editor.appendLoreSpacer
import kotlinx.coroutines.withContext
import com.charleskorn.kaml.YamlNode
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
        val rawValue = context.value.displayString()
        return stackedItem(config.iconMaterial) {
            displayName = EditorItemStyling.title(context.label)
            if (context.description.isNotEmpty()) {
                appendLoreSpacer()
                lore += EditorItemStyling.section("Description")
                lore += EditorItemStyling.wrappedHints(context.description)
            }
            appendLoreSpacer()
            lore += EditorItemStyling.wrappedValueLines("Value: ", EditorItemStyling.rawValue(rawValue))
            val preview = EditorItemStyling.formattedPreview(rawValue, force = config.showFormattedPreview)
            if (preview.isNotEmpty()) {
                appendLoreSpacer()
                lore += preview
            }
            appendLoreSpacer()
            lore += EditorItemStyling.section("Actions")
            lore += EditorItemStyling.wrappedActions(
                buildList {
                    add("Left click to edit")
                    if (context.descriptor.isNullable) {
                        add("Press Q to clear value")
                    }
                }
            )
        }.getItem()
    }

    final override suspend fun edit(player: Player, context: EditorFieldContext, config: C): FieldEditResult {
        EditorCloseGuard.suppress(player)
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }
        EditorChatMessages.sendPrompt(
            player = player,
            prompt = config.prompt,
            allowNull = context.descriptor.isNullable
        )
        val input = ChatInput.createHandle(listOf("cancel")).await(player) ?: return FieldEditResult.NoChange
        val prepared = prepareInput(player, input.trim(), context, config).fold(
            onSuccess = { it },
            onFailure = {
                EditorChatMessages.sendError(player, it.message ?: "Invalid value.")
                return FieldEditResult.NoChange
            }
        )
        return parse(prepared, context, config).fold(
            onSuccess = { FieldEditResult.Updated(it) },
            onFailure = {
                EditorChatMessages.sendError(player, it.message ?: "Invalid value.")
                FieldEditResult.NoChange
            }
        )
    }

    protected open suspend fun prepareInput(
        player: Player,
        raw: String,
        context: EditorFieldContext,
        config: C
    ): Result<String> = Result.success(raw)

    protected abstract suspend fun parse(raw: String, context: EditorFieldContext, config: C): Result<YamlNode>
}

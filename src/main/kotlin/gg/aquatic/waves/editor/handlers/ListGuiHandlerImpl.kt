package gg.aquatic.waves.editor.handlers

import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.ui.ConfigurableListMenu
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.ListGuiHandler
import org.bukkit.entity.Player

class ListGuiHandlerImpl<T>(private val context: EditorContext) : ListGuiHandler<T> {

    override suspend fun open(player: Player, editor: ListEditorValue<T>, updateParent: suspend () -> Unit) {
        context.navigate {
            // Pass the addButtonClick from the editor instance
            ConfigurableListMenu(context, editor, editor.addButtonClick, updateParent).open(player)
        }
    }
}

package gg.aquatic.waves.editor.handlers

import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.waves.editor.EditorContext
import gg.aquatic.waves.editor.ui.ConfigurableListMenu
import gg.aquatic.waves.editor.value.ListEditorValue
import gg.aquatic.waves.editor.value.ListGuiHandler
import org.bukkit.entity.Player

class ListGuiHandlerImpl<T>(private val context: EditorContext) : ListGuiHandler<T> {

    override fun open(player: Player, editor: ListEditorValue<T>, updateParent: () -> Unit) {
        val menu = ConfigurableListMenu(context, editor, updateParent)
        
        KMenuCtx.launch {
            menu.open(player)
        }
    }
}

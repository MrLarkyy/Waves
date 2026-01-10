package gg.aquatic.waves.editor

import org.bukkit.entity.Player
import java.util.*

class EditorContext(val player: Player) {
    private val history = Stack<suspend () -> Unit>()

    /**
     * Opens a new menu and saves the current one to history.
     * @param backLogic A lambda that re-opens the CURRENT menu.
     * @param nextOpen A lambda that opens the NEW menu.
     */
    suspend fun navigateTo(backLogic: suspend () -> Unit, nextOpen: suspend () -> Unit) {
        history.push(backLogic)
        nextOpen()
    }

    /**
     * Returns to the previous menu in the stack.
     */
    suspend fun goBack() {
        if (history.isNotEmpty()) {
            val lastMenu = history.pop()
            lastMenu()
        } else {
            player.closeInventory()
        }
    }
}

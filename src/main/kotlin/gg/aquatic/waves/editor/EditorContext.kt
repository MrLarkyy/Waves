package gg.aquatic.waves.editor

import gg.aquatic.common.coroutine.BukkitCtx
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.util.*

class EditorContext(val player: Player) {
    private val path = Stack<suspend () -> Unit>()

    /**
     * Navigates to a new menu.
     */
    suspend fun navigate(openLogic: suspend () -> Unit) {
        path.push(openLogic)
        openLogic()
    }

    /**
     * Returns to the previous menu.
     */
    suspend fun goBack() {
        if (path.size > 1) {
            path.pop() // Remove current
            val previous = path.peek()
            previous() // Open previous
        } else {
            path.clear()
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
        }
    }

    /**
     * Refreshes the current menu without affecting history.
     */
    suspend fun refresh() {
        if (path.isNotEmpty()) {
            path.peek().invoke()
        }
    }
}

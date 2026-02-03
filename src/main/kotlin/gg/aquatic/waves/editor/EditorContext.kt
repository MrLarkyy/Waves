package gg.aquatic.waves.editor

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.coroutine.VirtualsCtx
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.util.*

class EditorContext(
    val player: Player,
    var onSave: suspend () -> Unit = {}
) {
    internal val path = Stack<suspend () -> Unit>()
    private val navMutex = Mutex()

    /**
     * Navigates to a new menu.
     */
    suspend fun navigate(openLogic: suspend () -> Unit) {
        navMutex.withLock {
            path.push(openLogic)
        }
        openLogic()
    }

    /**
     * Returns to the previous menu.
     */
    suspend fun goBack() {
        val previous = navMutex.withLock {
            if (path.size > 1) {
                path.pop()
                path.peek()
            } else {
                path.clear()
                null
            }
        }
        if (previous != null) {
            previous()
        } else {
            withContext(BukkitCtx.ofEntity(player)) {
                player.closeInventory()
            }
        }
    }

    /**
     * Refreshes the current menu without affecting history.
     */
    suspend fun refresh() {
        val current = navMutex.withLock {
            if (path.isNotEmpty()) path.peek() else null
        }
        current?.invoke()
    }

    suspend fun save() = withContext(VirtualsCtx) {
        onSave()
    }.also {
        withContext(BukkitCtx.ofEntity(player)) {
            player.sendMessage("Changes saved successfully!")
        }
    }
}

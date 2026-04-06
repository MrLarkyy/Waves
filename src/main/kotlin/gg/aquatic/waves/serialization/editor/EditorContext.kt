package gg.aquatic.waves.serialization.editor

import gg.aquatic.common.coroutine.BukkitCtx
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.util.Stack

class EditorContext(
    val player: Player,
) {
    internal val path = Stack<suspend () -> Unit>()
    private val navMutex = Mutex()

    suspend fun navigate(openLogic: suspend () -> Unit) {
        navMutex.withLock {
            path.push(openLogic)
        }
        openLogic()
    }

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

    suspend fun refresh() {
        val current = navMutex.withLock {
            if (path.isNotEmpty()) path.peek() else null
        }
        current?.invoke()
    }
}

package gg.aquatic.waves.serialization.editor

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal object EditorCloseGuard {
    private val suppressed = ConcurrentHashMap<UUID, AtomicInteger>()

    fun suppress(player: Player) {
        suppressed.computeIfAbsent(player.uniqueId) { AtomicInteger(0) }.incrementAndGet()
    }

    fun consume(player: Player): Boolean {
        val counter = suppressed[player.uniqueId] ?: return false
        val remaining = counter.decrementAndGet()
        return when {
            remaining >= 0 -> {
                if (remaining == 0) {
                    suppressed.remove(player.uniqueId, counter)
                }
                true
            }
            else -> {
                suppressed.remove(player.uniqueId, counter)
                false
            }
        }
    }
}

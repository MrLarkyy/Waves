package gg.aquatic.waves.serialization.editor

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal object ActiveEditorContextRegistry {
    private val contexts = ConcurrentHashMap<UUID, EditorContext>()

    fun get(player: Player): EditorContext? = contexts[player.uniqueId]

    suspend fun <T> withContext(player: Player, context: EditorContext, block: suspend () -> T): T {
        contexts[player.uniqueId] = context
        return try {
            block()
        } finally {
            contexts.remove(player.uniqueId, context)
        }
    }
}

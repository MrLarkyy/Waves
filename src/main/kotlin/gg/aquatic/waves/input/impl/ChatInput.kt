package gg.aquatic.waves.input.impl

import gg.aquatic.common.event
import gg.aquatic.common.unregister
import gg.aquatic.waves.input.AwaitingInput
import gg.aquatic.waves.input.Input
import gg.aquatic.waves.input.InputHandle
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import kotlin.coroutines.resume

object ChatInput : Input {

    private var listener: Listener? = null

    override val awaiting = HashMap<Player, AwaitingInput>()

    private fun initialize() {
        listener = event<AsyncChatEvent> {
            val awaitingData = awaiting[it.player] ?: return@event
            it.isCancelled = true
            (awaitingData.handle as Handle).handle(it, awaitingData)
            if (awaiting.isEmpty()) terminate()
        }
    }

    private fun terminate() {
        listener?.unregister()
        listener = null
    }

    fun createHandle(
        cancelVariants: List<String> = listOf("cancel"),
        validator: ChatInputValidator? = null
    ): InputHandle {
        return Handle(cancelVariants, validator)
    }

    class Handle(
        private val cancelVariants: List<String> = listOf("cancel"),
        private val validator: ChatInputValidator? = null
    ) : InputHandle {
        override val input: Input = ChatInput

        override suspend fun await(player: Player): String? = suspendCancellableCoroutine { cont ->
            val handle = AwaitingInput(player, cont, this)
            awaiting += player to handle

            if (listener == null) {
                initialize()
            }

            cont.invokeOnCancellation {
                val existing = awaiting[player]
                if (existing === handle) {
                    awaiting.remove(player)
                    if (awaiting.isEmpty()) terminate()
                }
            }
        }

        fun handle(event: AsyncChatEvent, awaitingInput: AwaitingInput) {
            val content = event.signedMessage().message()

            if (content.lowercase() in cancelVariants) {
                if (awaitingInput.continuation.isActive) {
                    awaitingInput.continuation.resume(null)
                }
                awaiting.remove(event.player)
                return
            }

            if (validator != null && !validator.isValid(event.player, content)) {
                // Keep the entry in 'awaiting' so they can try again
                awaiting[event.player] = awaitingInput
                return
            }

            if (awaitingInput.continuation.isActive) {
                awaitingInput.continuation.resume(content)
            }
            awaiting.remove(event.player)
        }

        override fun forceCancel(player: Player) {
            val handle = awaiting[player] ?: return
            if (handle.continuation.isActive) {
                handle.continuation.resume(null)
            }
            awaiting -= player

            if (awaiting.isEmpty()) {
                terminate()
            }
        }
    }
}

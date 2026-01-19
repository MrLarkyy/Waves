package gg.aquatic.waves.input.impl

import gg.aquatic.common.event
import gg.aquatic.common.unregister
import gg.aquatic.waves.input.AwaitingInput
import gg.aquatic.waves.input.Input
import gg.aquatic.waves.input.InputHandle
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.util.concurrent.CompletableFuture

object ChatInput : Input {

    private var listener: Listener? = null

    override val awaiting = HashMap<Player, AwaitingInput>()

    private fun initialize() {
        listener = event<AsyncChatEvent> {
            val awaitingData = awaiting[it.player] ?: return@event
            it.isCancelled = true
            (awaitingData.handle as Handle).handle(it, awaitingData)

            if (awaitingData.future.isDone && awaiting[it.player] == awaitingData) {
                awaiting.remove(it.player)
                if (awaiting.isEmpty()) terminate()
            }
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

        override fun await(player: Player): CompletableFuture<String?> {
            val handle = AwaitingInput(player, CompletableFuture(), this)
            awaiting += player to handle

            if (listener == null) {
                initialize()
            }

            return handle.future
        }

        fun handle(event: AsyncChatEvent, awaitingInput: AwaitingInput) {
            val content = event.signedMessage().message()

            if (content.lowercase() in cancelVariants) {
                awaitingInput.future.complete(null)
                awaiting.remove(event.player)
                return
            }

            if (validator != null && !validator.isValid(event.player, content)) {
                // Keep the entry in 'awaiting' so they can try again
                awaiting[event.player] = awaitingInput
                return
            }

            awaitingInput.future.complete(content)
        }

        override fun forceCancel(player: Player) {
            val handle = awaiting[player] ?: return
            handle.future.complete(null)
            awaiting -= player

            if (awaiting.isEmpty()) {
                terminate()
            }
        }
    }
}
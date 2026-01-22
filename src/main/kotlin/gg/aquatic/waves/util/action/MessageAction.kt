package gg.aquatic.waves.util.action

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.execute.Action
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.waves.util.argument.MessageArgument
import gg.aquatic.waves.util.message.EmptyMessage
import org.bukkit.entity.Player

object MessageAction : Action<Player> {
    override suspend fun execute(
        binder: Player,
        args: ArgumentContext<Player>
    ) {
        val message = args.any("message") as? PaperMessage ?: return
        message.replace { str -> args.updater(binder, str)}.send(binder)
    }

    override val arguments: List<ObjectArgument<*>> = listOf(
        MessageArgument("message", EmptyMessage, true),
    )
}
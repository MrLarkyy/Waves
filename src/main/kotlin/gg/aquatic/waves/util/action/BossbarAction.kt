package gg.aquatic.waves.util.action

import gg.aquatic.common.toMMComponent
import gg.aquatic.execute.Action
import gg.aquatic.execute.argument.ArgumentContext
import gg.aquatic.execute.argument.ObjectArgument
import gg.aquatic.waves.Waves
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.function.Consumer

object BossbarAction : Action<Player> {

    override suspend fun execute(
        binder: Player,
        args: ArgumentContext<Player>
    ) {
        val message = args.string("message") ?: return
        val progress = args.float("progress") ?: 0.0f
        val color = BossBar.Color.valueOf((args.string("color") ?: "BLUE").uppercase())
        val style = BossBar.Overlay.valueOf((args.string("style") ?: "SOLID").uppercase())

        val bossbar = BossBar.bossBar(message.toMMComponent(), progress, color, style)
        bossbar.addViewer(binder)
        val duration = args.int("duration") ?: 60

        Bukkit.getGlobalRegionScheduler().runDelayed(Waves, Consumer {
            bossbar.removeViewer(binder)
        }, duration.toLong())
    }

    override val arguments: List<ObjectArgument<*>> = arguments {
        primitive("message", "", true)
        primitive("progress", 0.0f)
        primitive("color", "BLUE")
        primitive("style", "SOLID")
        primitive("duration", 60, true)
    }
}
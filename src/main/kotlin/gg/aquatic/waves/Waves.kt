package gg.aquatic.waves

import gg.aquatic.common.coroutine.SingleThreadedContext
import gg.aquatic.common.event
import gg.aquatic.common.initializeCommon
import gg.aquatic.execute.Action
import gg.aquatic.execute.Execute
import gg.aquatic.execute.action.registerAction
import gg.aquatic.execute.initExecute
import gg.aquatic.kholograms.HologramHandler
import gg.aquatic.klocale.LocaleManager
import gg.aquatic.klocale.impl.paper.KLocale
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.initializeKMenu
import gg.aquatic.kregistry.Registry
import gg.aquatic.pakket.Pakket
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.statistik.initializeStatistik
import gg.aquatic.waves.input.InputHandler
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.testing.data.TestingEditor
import gg.aquatic.waves.util.action.BossbarAction
import gg.aquatic.waves.util.action.MessageAction
import gg.aquatic.waves.world.AwaitingWorlds
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

object Waves : JavaPlugin() {

    override fun onLoad() {

    }

    lateinit var locale: LocaleManager<PaperMessage>

    override fun onEnable() {
        initializeCommon(this)

        event<PlayerJoinEvent> {
            Pakket.handler.injectPacketListener(it.player)
        }
        event<PlayerQuitEvent> {
            Pakket.handler.unregisterPacketListener(it.player)
        }
        initializeStacked(this, SingleThreadedContext("stacked").scope)
        initializeKMenu(this, SingleThreadedContext("kmenu").scope)
        initExecute(this)
        Registry.update {
            Execute.injectExecutables(this)
            replaceRegistry(Action.REGISTRY_KEY) {
                registerAction("message", MessageAction)
                registerAction("bossbar", BossbarAction)
            }
        }

        InputHandler.initialize(mapOf("chat" to ChatInput))
        AwaitingWorlds.initialize()
        initializeStatistik()
        HologramHandler.initialize()

        TestingEditor.initialize()

        locale = KLocale.paper {}

    }
}
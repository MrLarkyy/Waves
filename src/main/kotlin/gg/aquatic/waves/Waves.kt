package gg.aquatic.waves

import gg.aquatic.clientside.initializeClientside
import gg.aquatic.common.MiniMessageResolver
import gg.aquatic.common.coroutine.SingleThreadedContext
import gg.aquatic.common.event
import gg.aquatic.common.initializeCommon
import gg.aquatic.execute.Action
import gg.aquatic.execute.initializeExecute
import gg.aquatic.kholograms.HologramHandler
import gg.aquatic.klocale.LocaleManager
import gg.aquatic.klocale.impl.paper.KLocale
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.kmenu.initializeKMenu
import gg.aquatic.kregistry.bootstrap.BootstrapHolder
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.pakket.Pakket
import gg.aquatic.quickminimessage.MMParser
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.statistik.initializeStatistik
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.input.initializeInput
import gg.aquatic.waves.testing.data.TestingEditor
import gg.aquatic.waves.util.action.BossbarAction
import gg.aquatic.waves.util.action.MenuNextPageAction
import gg.aquatic.waves.util.action.MenuPreviousPageAction
import gg.aquatic.waves.util.action.MessageAction
import gg.aquatic.waves.world.AwaitingWorlds
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

object Waves : JavaPlugin(), BootstrapHolder, RegistryHolder {

    private lateinit var registriesInject: () -> Unit

    override fun onLoad() {
        registriesInject = inject()
    }

    lateinit var locale: LocaleManager<PaperMessage>

    override fun onEnable() {
        val mmResolver = MiniMessageResolver {
            MMParser.parse(it)
        }

        initializeCommon(this, mmResolver)

        event<PlayerJoinEvent> {
            Pakket.handler.injectPacketListener(it.player)
        }
        event<PlayerQuitEvent> {
            Pakket.handler.unregisterPacketListener(it.player)
        }
        initializeStacked(this, SingleThreadedContext("stacked").scope, mmResolver)
        initializeKMenu(SingleThreadedContext("kmenu").scope)
        initializeExecute(this, mmResolver)

        registryBootstrap(this) {
            registry(Action.REGISTRY_KEY) {
                add("message", MessageAction)
                add("bossbar", BossbarAction)
                add("next-page", MenuNextPageAction)
                add("previous-page", MenuPreviousPageAction)
            }
        }

        initializeClientside()
        initializeInput(mapOf("chat" to ChatInput))
        AwaitingWorlds.initialize()
        initializeStatistik(emptyMap())
        HologramHandler.initialize()

        TestingEditor.initialize()

        locale = KLocale.paper {}
        registriesInject()
    }
}
package gg.aquatic.waves

import gg.aquatic.common.deepFilesLookup
import gg.aquatic.common.event
import gg.aquatic.common.initializeCommon
import gg.aquatic.execute.Action
import gg.aquatic.execute.Execute
import gg.aquatic.execute.action.registerAction
import gg.aquatic.execute.initExecute
import gg.aquatic.klocale.LocaleProvider
import gg.aquatic.klocale.impl.paper.KLocale
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.klocale.impl.paper.provider.YamlLocaleProvider
import gg.aquatic.klocale.provider.MergedLocaleProvider
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.kregistry.Registry
import gg.aquatic.pakket.Pakket
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.waves.input.InputHandler
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.statistic.initializeStatistik
import gg.aquatic.waves.util.action.BossbarAction
import gg.aquatic.waves.util.action.MessageAction
import gg.aquatic.waves.world.AwaitingWorlds
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

object Waves : JavaPlugin() {

    override fun onLoad() {

    }

    override fun onEnable() {
        initializeCommon(this)

        event<PlayerJoinEvent> {
            Pakket.handler.injectPacketListener(it.player)
        }
        event<PlayerQuitEvent> {
            Pakket.handler.unregisterPacketListener(it.player)
        }
        initializeStacked(this, KMenuCtx.scope)
        KMenu.initialize()
        initExecute(this)
        Registry.update {
            Execute.injectExecutables(this)
            replaceRegistry(Action.REGISTRY_KEY) {
                registerAction("message", MessageAction)
                registerAction("bossbar", BossbarAction)
            }
        }

        InputHandler.initialize(mapOf("chat" to ChatInput))
        //TestingEditor.initialize()
        AwaitingWorlds.initialize()
        initializeStatistik()
    }

    fun initializeMultilingualMessages(plugin: JavaPlugin) {
        val dataFolder = plugin.dataFolder
        dataFolder.mkdirs()
        val languageFiles = dataFolder.resolve("messages").deepFilesLookup { it.extension == "yml" }

        val providers = ArrayList<LocaleProvider<PaperMessage>>()
        for (item in languageFiles) {
            providers += YamlLocaleProvider(item, YamlLocaleProvider.DefaultSerializer)
        }

        KLocale.paper(
            MergedLocaleProvider(providers)
        ) {}
    }

}
package gg.aquatic.waves

import gg.aquatic.common.event
import gg.aquatic.common.initializeCommon
import gg.aquatic.execute.initExecute
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.pakket.Pakket
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.waves.testing.data.TestingEditor
import gg.aquatic.waves.input.InputHandler
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

        InputHandler.initialize()
        TestingEditor.initialize()
        AwaitingWorlds.initialize()
    }

}
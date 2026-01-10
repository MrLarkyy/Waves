package gg.aquatic.waves

import gg.aquatic.execute.initExecute
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.coroutine.KMenuCtx
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.waves.input.InputHandler
import org.bukkit.plugin.java.JavaPlugin

object Waves : JavaPlugin() {

    override fun onLoad() {

    }

    override fun onEnable() {
        KMenu.initialize()
        initializeStacked(this, KMenuCtx.scope)
        initExecute(this)
        InputHandler.initialize()
    }

}
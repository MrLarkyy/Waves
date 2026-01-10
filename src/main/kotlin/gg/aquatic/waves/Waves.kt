package gg.aquatic.waves

import gg.aquatic.waves.input.InputHandler
import org.bukkit.plugin.java.JavaPlugin

object Waves : JavaPlugin() {

    override fun onLoad() {

    }

    override fun onEnable() {
        InputHandler.initialize()


    }

}
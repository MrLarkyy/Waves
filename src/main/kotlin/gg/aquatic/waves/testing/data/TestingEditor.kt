package gg.aquatic.waves.testing.data

import gg.aquatic.kommand.command
import gg.aquatic.waves.Config
import gg.aquatic.waves.Waves
import gg.aquatic.waves.editor.EditorHandler
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object TestingEditor {

    fun initialize() {

        command("edit") {
            execute<Player> {

                Waves.dataFolder.mkdirs()
                val config = Config("test.yml", Waves)
                config.loadSync()

                val cfg = config.configuration

                val itemData = BaseCrateData("test", Component.text("Test Crate"), listOf())
                itemData.deserialize(cfg)
                EditorHandler.startEditing(sender, Component.text("Item Edit"), itemData) {
                    it.serialize(cfg)
                    config.saveSync()
                    sender.sendMessage("Saved: ${cfg.saveToString()}")
                }

                Bukkit.getGlobalRegionScheduler()

                true
            }
        }
    }
}
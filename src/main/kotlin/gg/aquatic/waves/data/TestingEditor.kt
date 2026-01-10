package gg.aquatic.waves.data

import gg.aquatic.kommand.command
import gg.aquatic.waves.editor.EditorHandler
import net.kyori.adventure.text.Component
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

object TestingEditor {

    fun initialize() {

        command("edit") {
            execute<Player> {

                val itemData = ItemData()
                EditorHandler.startEditing(sender, Component.text("Item Edit"), itemData) {
                    val section = YamlConfiguration()
                    it.serialize(section)

                    sender.sendMessage("Saved: ${section.saveToString()}")
                }

                true
            }
        }

    }

}
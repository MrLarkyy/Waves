package gg.aquatic.waves.testing.data

import gg.aquatic.common.Config
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.kmenu.menu.component.Button
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.kommand.command
import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.sendPacket
import gg.aquatic.replace.PlaceholderContext
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.Waves
import gg.aquatic.waves.editor.EditorHandler
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object TestingEditor {

    fun initialize() {

        command("anvil") {
            execute<Player> {
                val menu = PrivateMenu(Component.text("Anvil Menu"), InventoryType.ANVIL.apply {
                    this.onRename = { player, name, inventory ->
                        player.sendMessage("Renamed to: $name")
                        val packet = Pakket.handler.createContainerPropertyPacket(126, 0, -1)
                        player.sendPacket(packet, true)
                    }
                }, sender, true)

                KMenu.scope.launch {
                    menu.addComponent(Button("example", stackedItem(Material.STONE) {
                        displayName = Component.text("Write: ")
                    }.getItem(), listOf(0), 1, -1, textUpdater = PlaceholderContext.privateMenu()))

                    menu.open()
                }
                true
            }
        }

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
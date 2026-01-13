package gg.aquatic.waves.util

import gg.aquatic.waves.Waves
import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

object PAPIUtil {
    fun registerExtension(author: String, identifier: String, onRequest: (player: OfflinePlayer, params: String) -> String) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return
        }
        val extension = object : PlaceholderExpansion() {
            override fun getIdentifier(): String {
                return identifier
            }

            override fun getAuthor(): String {
                return author
            }

            override fun getVersion(): String {
                return "1.0.0"
            }

            override fun canRegister(): Boolean {
                return true
            }

            override fun persist(): Boolean {
                return false
            }

            override fun onRequest(player: OfflinePlayer, params: String): String {
                return onRequest(player, params)
            }
        }
        extension.register()
    }
}

fun String.updatePAPIPlaceholders(player: Player): String {
    Waves.server.pluginManager.getPlugin("PlaceholderAPI") ?: return this
    return PlaceholderAPI.setPlaceholders(player, this)
}
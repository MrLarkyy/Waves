package gg.aquatic.waves.hologram.serialize

import gg.aquatic.kregistry.RegistryId
import gg.aquatic.kregistry.RegistryKey
import gg.aquatic.waves.hologram.CommonHologramLineSettings
import org.bukkit.configuration.ConfigurationSection

interface LineFactory {

    fun load(section: ConfigurationSection, commonOptions: CommonHologramLineSettings): LineSettings?

    companion object {
        val REGISTRY = mutableMapOf<String, LineFactory>()
        val REGISTRY_KEY = RegistryKey<String, LineFactory>(RegistryId("aquatic", "line-factories"))
    }
}
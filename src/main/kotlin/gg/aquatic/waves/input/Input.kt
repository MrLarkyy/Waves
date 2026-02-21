package gg.aquatic.waves.input

import gg.aquatic.kregistry.core.Registry
import gg.aquatic.kregistry.core.RegistryId
import gg.aquatic.kregistry.core.RegistryKey
import gg.aquatic.waves.Waves
import org.bukkit.entity.Player

interface Input {

    val awaiting: Map<Player, AwaitingInput>

    fun forceCancel(player: Player) {
        awaiting[player]?.handle?.forceCancel(player)
    }

    companion object {
        val REGISTRY_KEY = RegistryKey.simple<String, Input>(RegistryId("aquatic", "input"))

        val REGISTRY: Registry<String, Input>
            get() {
                return Waves[REGISTRY_KEY]
            }
    }
}

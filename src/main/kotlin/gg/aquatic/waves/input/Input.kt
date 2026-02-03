package gg.aquatic.waves.input

import gg.aquatic.common.toMMComponent
import gg.aquatic.kregistry.FrozenRegistry
import gg.aquatic.kregistry.Registry
import gg.aquatic.kregistry.RegistryId
import gg.aquatic.kregistry.RegistryKey
import org.bukkit.Material
import org.bukkit.entity.Player

interface Input {

    val awaiting: Map<Player, AwaitingInput>

    fun forceCancel(player: Player) {
        awaiting[player]?.handle?.forceCancel(player)
    }

    companion object {
        val REGISTRY_KEY = RegistryKey<String, Input>(RegistryId("aquatic", "input"))

        val REGISTRY: FrozenRegistry<String, Input>
            get() {
                return Registry[REGISTRY_KEY]
            }
    }
}

interface InputHandle {
    val input: Input
    suspend fun await(player: Player): String?
    fun forceCancel(player: Player)

    suspend fun awaitMaterial(player: Player): Material? =
        await(player)?.let { Material.matchMaterial(it) }

    suspend fun awaitInt(player: Player): Int? =
        await(player)?.toIntOrNull()

    suspend fun awaitDouble(player: Player): Double? =
        await(player)?.toDoubleOrNull()

    suspend fun awaitBoolean(player: Player): Boolean? =
        await(player)?.toBooleanStrictOrNull()

    suspend fun awaitMMComponent(player: Player): net.kyori.adventure.text.Component? =
        await(player)?.toMMComponent()
}

class AwaitingInput(
    val player: Player,
    val continuation: kotlinx.coroutines.CancellableContinuation<String?>,
    val handle: InputHandle
)

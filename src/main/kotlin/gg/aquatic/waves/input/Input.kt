package gg.aquatic.waves.input

import gg.aquatic.common.toMMComponent
import gg.aquatic.kregistry.FrozenRegistry
import gg.aquatic.kregistry.Registry
import gg.aquatic.kregistry.RegistryId
import gg.aquatic.kregistry.RegistryKey
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

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
    fun await(player: Player): CompletableFuture<String?>
    fun forceCancel(player: Player)

    fun awaitMaterial(player: Player): CompletableFuture<Material?> =
        await(player).thenApply { it?.let { Material.matchMaterial(it) } }

    fun awaitInt(player: Player): CompletableFuture<Int?> =
        await(player).thenApply { it?.toIntOrNull() }

    fun awaitDouble(player: Player): CompletableFuture<Double?> =
        await(player).thenApply { it?.toDoubleOrNull() }

    fun awaitBoolean(player: Player): CompletableFuture<Boolean?> =
        await(player).thenApply { it?.toBooleanStrictOrNull() }

    fun awaitMMComponent(player: Player): CompletableFuture<net.kyori.adventure.text.Component?> =
        await(player).thenApply { it?.toMMComponent() }
}

class AwaitingInput(
    val player: Player,
    val future: CompletableFuture<String?>,
    val handle: InputHandle
)
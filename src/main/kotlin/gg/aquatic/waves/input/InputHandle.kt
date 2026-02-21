package gg.aquatic.waves.input

import gg.aquatic.common.toMMComponent
import org.bukkit.Material
import org.bukkit.entity.Player

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

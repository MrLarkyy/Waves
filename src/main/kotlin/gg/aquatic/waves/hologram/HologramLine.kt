package gg.aquatic.waves.hologram

import gg.aquatic.pakket.api.nms.PacketEntity
import gg.aquatic.pakket.api.nms.entity.EntityDataValue
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Player
import org.joml.Vector3f

interface HologramLine {

    var scale: Float
    var billboard: Billboard
    var transformationDuration: Int
    var teleportInterpolation: Int
    var translation: Vector3f

    val height: Double
    val filter: suspend (Player) -> Boolean

    val failLine: HologramLine?

    suspend fun getVisibleLine(player: Player): HologramLine? =
        if (filter(player)) {
            this
        } else {
            failLine?.getVisibleLine(player)
        }

    fun spawn(location: Location, player: Player, placeholderContext: PlaceholderContext<Player>): PacketEntity
    fun tick(spawnedHologramLine: SpawnedHologramLine)
    fun buildData(placeholderContext: PlaceholderContext<Player>, player: Player): List<EntityDataValue>

    fun buildData(spawnedHologramLine: SpawnedHologramLine): List<EntityDataValue> {
        return buildData(spawnedHologramLine.placeholderContext, spawnedHologramLine.player)
    }
}
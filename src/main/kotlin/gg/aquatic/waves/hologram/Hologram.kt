package gg.aquatic.waves.hologram

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.execute.checkConditions
import gg.aquatic.execute.requirement.ConditionHandle
import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.sendPacket
import gg.aquatic.replace.PlaceholderContext
import gg.aquatic.waves.hologram.line.TextHologramLine
import gg.aquatic.waves.hologram.serialize.LineSettings
import gg.aquatic.waves.util.chunk.chunkId
import gg.aquatic.waves.util.chunk.trackedBy
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class Hologram(
    location: Location,
    val filter: suspend (Player) -> Boolean,
    val placeholderContext: () -> PlaceholderContext<Player>,
    val viewDistance: Int,
    lines: Collection<HologramLine>,
) {

    var chunk: Chunk? = if (location.chunk.isLoaded) location.chunk else null
    var seat: Int? = null
        private set

    fun setAsPassenger(seat: Int?) {
        val previous = this.seat
        this.seat = seat
        viewers.forEach { (viewer, lines) ->
            if (seat != null) {
                val ids = lines.lines.map { it.packetEntity.entityId }.toIntArray()
                val passengerPacket = Pakket.handler.createPassengersPacket(seat,ids)
                viewer.sendPacket(passengerPacket, false)
            } else if (previous != null) {
                val passengerPacket = Pakket.handler.createPassengersPacket(previous,intArrayOf())
                viewer.sendPacket(passengerPacket, false)
            }
        }
    }

    suspend fun setLines(lines: Collection<HologramLine>) {
        this.lines.clear()
        this.lines.addAll(lines)

        tickRange()
        destroyLines()
        for (player in viewers.keys) {
            showOrUpdate(player)
        }
    }

    fun setLineText(lineIndex: Int, text: String) {
        val line = lines.elementAtOrNull(lineIndex) ?: return
        if (line !is TextHologramLine) return

        line.text = text
    }

    fun setTeleportInterpolation(interpolation: Int) {
        for (line in lines) {
            line.teleportInterpolation = interpolation
        }
    }

    fun setTransformationInterpolationDuration(duration: Int) {
        for (line in lines) {
            line.transformationDuration = duration
        }
    }

    fun setScale(scale: Float) {
        for (line in lines) {
            line.scale = scale
        }
    }

    var location = location
        private set

    @Volatile
    private var rangeTick = 0

    val lines = ConcurrentHashMap.newKeySet<HologramLine>().apply { addAll(lines) }
    val viewers = ConcurrentHashMap<Player, HologramViewer>()

    init {
        val chunkId = location.chunk.chunkId()
        if (this.chunk == null) {
            HologramHandler.waitingHolograms.getOrPut(chunkId) { ArrayList() }.add(this)
        } else {
            HologramHandler.tickingHolograms.getOrPut(chunkId) { ArrayList() }.add(this)
            VirtualsCtx {
                checkPlayersRange()
                tick()
            }
        }
    }

    suspend fun tick() {
        tickRange()
        viewers.forEach { (player, _) ->
            // CurrentLineIndex -> Get Hologram line -> Compare Hologram Line with SpawnedHologramLine
            // If it is the same, then skip & add index, otherwise update line, add to the set & move other lines

            // First process all line text update & visibility and then apply changes

            showOrUpdate(player)
        }
    }

    suspend fun showOrUpdate(player: Player) {
        val viewer = viewers.getOrPut(player) { HologramViewer(player, placeholderContext(), mutableSetOf()) }
        val lines = viewer.lines

        suspend fun getVisibleLine(player: Player, hologramLine: HologramLine): HologramLine? {
            if (hologramLine.filter(player)) {
                return hologramLine
            }
            return getVisibleLine(player, hologramLine.failLine ?: return null)
        }

        val remainingLines = lines.toMutableSet()
        val newLines = mutableMapOf<HologramLine, SpawnedHologramLine?>()
        for (line in this.lines) {
            val visibleLine = getVisibleLine(player, line) ?: continue
            val spawnedLine = lines.find { it.line == visibleLine }
            newLines[visibleLine] = spawnedLine
            remainingLines.remove(spawnedLine ?: continue)
        }

        for (remainingLine in remainingLines) {
            remainingLine.destroy()
            lines.remove(remainingLine)
        }

        var changed = false

        var height = 0.0
        for ((line, spawnedLine) in newLines) {
            val halfHeight = line.height / 2.0
            height += halfHeight
            val location = this.location.clone().add(0.0, height, 0.0)
            if (spawnedLine == null) {
                val packetEntity = line.spawn(location, player, viewer.context)
                val newLine = SpawnedHologramLine(this, player, line, location, viewer.context, packetEntity)
                lines.add(newLine)
                changed = true
            } else {
                spawnedLine.tick()
                if (spawnedLine.currentLocation == location) continue
                spawnedLine.move(location)
            }
        }

        if (changed && seat != null) {
            val ids = lines.map { it.packetEntity.entityId }.toIntArray()
            val passengerPacket = Pakket.handler.createPassengersPacket(seat!!,ids)
            viewer.player.sendPacket(passengerPacket, false)
        }
    }

    private suspend fun tickRange() {
        rangeTick++
        if (rangeTick < 5) {
            return
        }
        rangeTick = 0
        checkPlayersRange()
    }

    suspend fun checkPlayersRange() {
        val remaining = viewers.toMutableMap()
        val chunk = this.chunk
        if (chunk != null) {
            for (trackedByPlayer in chunk.trackedBy()) {
                if (!filter(trackedByPlayer)) continue
                if (trackedByPlayer.world != location.world) continue
                if (trackedByPlayer.location.distanceSquared(location) <= viewDistance * viewDistance) {
                    remaining.remove(trackedByPlayer)
                    if (viewers.containsKey(trackedByPlayer)) {
                        continue
                    }
                    showOrUpdate(trackedByPlayer)
                }
            }
        }

        for (removed in remaining) {
            removed.value.lines.forEach { it.destroy() }
            viewers.remove(removed.key)
        }
    }

    fun destroyLines() {
        viewers.forEach { (_, spawnedHologramLines) ->
            spawnedHologramLines.lines.forEach { it.destroy() }
            spawnedHologramLines.lines.clear()
        }
    }

    fun destroy() {
        HologramHandler.removeHologram(this)
        destroyLines()
        viewers.clear()
        lines.clear()

    }

    suspend fun teleport(location: Location) {
        this.location = location
        viewers.forEach { (player, _) ->
            showOrUpdate(player)
        }
    }

    class Settings(
        val lines: List<LineSettings>,
        val conditions: List<ConditionHandle<Player>>,
        val viewDistance: Int,
    ) {
        fun create(
            location: Location,
            placeholderContext: () -> PlaceholderContext<Player>,
            filter: (Player) -> Boolean = { true },
        ): Hologram = Hologram(
            location,
            { p ->
                filter(p) && conditions.checkConditions(p)
            },
            placeholderContext,
            viewDistance,
            lines.map { it.create() }.toSet()
        )
    }

    class HologramViewer(
        val player: Player,
        val context: PlaceholderContext<Player>,
        val lines: MutableSet<SpawnedHologramLine>
    )
}
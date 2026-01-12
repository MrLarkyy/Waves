package gg.aquatic.waves.clientside.entity

import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.api.nms.PacketEntity
import gg.aquatic.pakket.api.nms.entity.EntityDataValue
import gg.aquatic.pakket.sendPacket
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.audience.FilterAudience
import gg.aquatic.waves.clientside.EntityBased
import gg.aquatic.waves.clientside.FakeObject
import gg.aquatic.waves.clientside.FakeObjectHandler
import gg.aquatic.waves.clientside.entity.data.impl.ItemEntityData
import gg.aquatic.waves.util.chunk.trackedBy
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class FakeEntity(
    val type: EntityType, location: Location,
    override val viewRange: Int,
    audience: AquaticAudience,
    consumer: FakeEntity.() -> Unit = {},
    override var onInteract: (FakeEntityInteractEvent) -> Unit = {},
    var onUpdate: (Player) -> Unit = {},
) : FakeObject(), EntityBased {

    @Volatile
    private var _audience: AquaticAudience = FilterAudience { false }

    override val audience: AquaticAudience
        get() = _audience

    override fun setAudience(audience: AquaticAudience) {
        this._audience = audience
        val viewers = viewers()
        for (viewer in viewers) {
            if (audience.canBeApplied(viewer) && viewer.isOnline) continue
            removeViewer(viewer)
        }
        for (player in
        location.world!!.players.filter { !viewers.contains(it) }) {
            if (!audience.canBeApplied(player)) continue
            addViewer(player)
        }
    }

    override var location: Location = location
        set(value) {
            field = value
            packetEntity = createEntity()
        }

    private var packetEntity = createEntity()

    private fun createEntity(): PacketEntity {
        val entity = Pakket.handler.createEntity(location, type) ?: throw Exception("Failed to create entity")
        return entity
    }

    override fun destroy() {
        destroyed = true
        for (player in isViewing()) {
            hide(player)
        }
        FakeObjectHandler.tickableObjects -= this
        unregister()
        FakeObjectHandler.idToEntity -= entityId
    }

    override val entityId: Int get() = packetEntity.entityId

    val entityData = ConcurrentHashMap<Int, EntityDataValue>()
    val equipment = ConcurrentHashMap<EquipmentSlot, ItemStack>()
    val passengers = ConcurrentHashMap.newKeySet<Int>()

    fun setEntityData(dataValue: EntityDataValue) {
        entityData += dataValue.id to dataValue
    }

    fun setEntityData(vararg dataValue: EntityDataValue) {
        for (data in dataValue) {
            setEntityData(data)
        }
    }

    fun setEntityData(dataValues: Collection<EntityDataValue>) {
        for (data in dataValues) {
            setEntityData(data)
        }
    }

    init {
        if (type == EntityType.ITEM) {
            setEntityData(ItemEntityData.Item.generate(ItemStack(Material.STONE)))
        }
        updateEntity(consumer)
        this._audience = audience
        FakeObjectHandler.tickableObjects += this
        FakeObjectHandler.idToEntity += entityId to this

        val chunkViewers = location.chunk.trackedBy().toSet()
        for (viewer in viewers()) {
            if (viewer in chunkViewers) {
                show(viewer)
            }
        }
    }

    fun register() {
        if (registered) return
        registered = true
        val chunk = location.chunk

        val bundle = FakeObjectHandler.getOrCreateChunkCacheBundle(
            chunk.x, chunk.z, chunk.world
        )
        bundle.entities += this
    }

    fun unregister() {
        if (!registered) return
        registered = false
        val chunk = location.chunk
        val bundle = FakeObjectHandler.getChunkCacheBundle(
            chunk.x, chunk.z, chunk.world
        ) ?: return
        bundle.entities -= this
    }

    fun updateEntity(func: FakeEntity.() -> Unit) {
        val hadPassengers = passengers.isNotEmpty()
        func(this)

        val updatePacket = Pakket.handler.createEntityUpdatePacket(packetEntity.entityId, entityData.values)
        packetEntity.updatePacket = updatePacket

        if (passengers.isNotEmpty()) {
            val passengersPacket = Pakket.handler.createPassengersPacket(packetEntity.entityId, passengers.toIntArray())
            packetEntity.passengerPacket = passengersPacket
        }

        packetEntity.equipment.clear()
        packetEntity.equipment += equipment

        val players = isViewing().toTypedArray()
        packetEntity.sendDataUpdate(Pakket.handler, false, *players)
        if (!(!hadPassengers && passengers.isEmpty())) {
            packetEntity.sendPassengerUpdate(Pakket.handler, false, *players)
        }
        packetEntity.sendEquipmentUpdate(Pakket.handler, *players)
    }

    private fun sendUpdate(player: Player) {
        onUpdate(player)
        if (entityData.isNotEmpty()) {
            packetEntity.sendDataUpdate(Pakket.handler, false, player)
        }
        packetEntity.equipment += equipment
        packetEntity.sendPassengerUpdate(Pakket.handler, false, player)
        packetEntity.sendEquipmentUpdate(Pakket.handler, player)
    }

    override fun addViewer(player: Player) {
        if (viewers().contains(player)) return
        addViewer(player)
        if (player.world.name != location.world!!.name) return
        if (player.location.distanceSquared(location) <= viewRange * viewRange) {
            show(player)
        }
    }

    override fun removeViewer(uuid: UUID) {
        removeViewer(uuid)
        removeIsViewing(uuid)
    }

    override fun removeViewer(player: Player) {
        if (isViewing().contains(player)) {
            hide(player)
        }
        FakeObjectHandler.handlePlayerRemove(player, this, true)
    }

    override fun show(player: Player) {
        if (isViewing().contains(player)) return
        setIsViewing(true, player)

        onUpdate(player)
        packetEntity.sendSpawnComplete(Pakket.handler, false, player)
    }

    override fun hide(player: Player) {
        setIsViewing(false, player)
        packetEntity.sendDespawn(Pakket.handler, false, player)
    }

    override suspend fun tick() {

    }

    fun teleport(location: Location) {
        this.location = location
        if (registered) {
            unregister()
            register()
        }
        val packet = Pakket.handler.createTeleportPacket(packetEntity.entityId, location)
        for (player in isViewing()) {
            player.sendPacket(packet, false)
        }
    }
}
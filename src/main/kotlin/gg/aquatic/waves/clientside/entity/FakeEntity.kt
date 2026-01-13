package gg.aquatic.waves.clientside.entity

import gg.aquatic.pakket.Pakket
import gg.aquatic.pakket.api.nms.PacketEntity
import gg.aquatic.pakket.api.nms.entity.EntityDataValue
import gg.aquatic.pakket.sendPacket
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.clientside.EntityBased
import gg.aquatic.waves.clientside.FakeObject
import gg.aquatic.waves.clientside.FakeObjectHandler
import gg.aquatic.waves.clientside.entity.data.impl.ItemEntityData
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

class FakeEntity(
    val type: EntityType,
    location: Location,
    override val viewRange: Int,
    audience: AquaticAudience,
    consumer: FakeEntity.() -> Unit = {},
    override var onInteract: (FakeEntityInteractEvent) -> Unit = {},
    var onUpdate: (Player) -> Unit = {},
    var onTick: suspend () -> Unit = {}
) : FakeObject(viewRange, audience), EntityBased {

    override var location: Location = location
        set(value) {
            field = value
            packetEntity = createPacketEntity()
        }

    private var packetEntity = createPacketEntity()
    override val entityId: Int get() = packetEntity.entityId

    val entityData = ConcurrentHashMap<Int, EntityDataValue>()
    val equipment = ConcurrentHashMap<EquipmentSlot, ItemStack>()
    val passengers = ConcurrentHashMap.newKeySet<Int>()

    init {
        if (type == EntityType.ITEM) {
            setEntityData(ItemEntityData.Item.generate(ItemStack(Material.STONE)))
        }
        updateEntity(consumer)
        setAudience(audience)
    }

    private fun createPacketEntity(): PacketEntity {
        return Pakket.handler.createEntity(location, type) ?: throw Exception("Failed to create NMS entity")
    }

    fun setEntityData(vararg dataValues: EntityDataValue) {
        dataValues.forEach { entityData[it.id] = it }
    }

    fun setEntityData(dataValues: Collection<EntityDataValue>) {
        dataValues.forEach { entityData[it.id] = it }
    }

    fun updateEntity(func: FakeEntity.() -> Unit) {
        val hadPassengers = passengers.isNotEmpty()
        func(this)

        packetEntity.updatePacket = Pakket.handler.createEntityUpdatePacket(entityId, entityData.values)
        if (passengers.isNotEmpty()) {
            packetEntity.passengerPacket = Pakket.handler.createPassengersPacket(entityId, passengers.toIntArray())
        }

        packetEntity.equipment.clear()
        packetEntity.equipment += equipment

        val viewers = isViewing.toTypedArray()
        packetEntity.sendDataUpdate(Pakket.handler, false, *viewers)
        if (hadPassengers || passengers.isNotEmpty()) {
            packetEntity.sendPassengerUpdate(Pakket.handler, false, *viewers)
        }
        packetEntity.sendEquipmentUpdate(Pakket.handler, *viewers)
    }

    override fun onShow(player: Player) {
        onUpdate(player)
        packetEntity.sendSpawnComplete(Pakket.handler, false, player)
    }

    override fun onHide(player: Player) {
        packetEntity.sendDespawn(Pakket.handler, false, player)
    }

    override fun handleInteract(player: Player, isLeftClick: Boolean) {
        val event = FakeEntityInteractEvent(this, player, isLeftClick)
        onInteract.invoke(event)
    }

    fun teleport(newLocation: Location) {
        this.location = newLocation
        if (registered) {
            unregister()
            register()
        }
        val packet = Pakket.handler.createTeleportPacket(entityId, newLocation)
        isViewing.forEach { it.sendPacket(packet, false) }
    }

    fun register() {
        if (registered) return
        registered = true
        FakeObjectHandler.tickableObjects += this
        FakeObjectHandler.idToEntity += entityId to this
        val bundle = FakeObjectHandler.getOrCreateChunkCacheBundle(location.chunk.x, location.chunk.z, location.world!!)
        bundle.entities += this
    }

    fun unregister() {
        if (!registered) return
        registered = false
        val bundle =
            FakeObjectHandler.getChunkCacheBundle(location.chunk.x, location.chunk.z, location.world!!) ?: return
        bundle.entities -= this
    }

    override fun destroy() {
        destroyed = true
        isViewing.forEach { hide(it) }
        FakeObjectHandler.tickableObjects -= this
        FakeObjectHandler.idToEntity -= entityId
        unregister()
    }

    override suspend fun tick() {
        onTick()
    }
}
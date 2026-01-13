package gg.aquatic.waves.interactable.type

import com.destroystokyo.paper.profile.PlayerProfile
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.ModeledEntity
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes
import com.ticxo.modelengine.api.model.bone.type.PlayerLimb
import gg.aquatic.waves.audience.AquaticAudience
import gg.aquatic.waves.interactable.Interactable
import gg.aquatic.waves.interactable.InteractableInteractEvent
import gg.aquatic.waves.interactable.InteractableModule
import gg.aquatic.waves.interactable.MEGInteractableDummy
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

class MEGInteractable(
    override val location: Location,
    val modelId: String,
    initialAudience: AquaticAudience,
    override val onInteract: (InteractableInteractEvent) -> Unit,
) : Interactable() {

    private val _viewers = ConcurrentHashMap.newKeySet<UUID>()
    override val viewers: Collection<Player> get() = _viewers.mapNotNull { Bukkit.getPlayer(it) }

    override var audience: AquaticAudience = initialAudience
        set(value) {
            field = value
            refreshViewers()
        }

    val dummy = MEGInteractableDummy(this).apply {
        location = this@MEGInteractable.location
        val rot = this@MEGInteractable.location
        bodyRotationController.yBodyRot = rot.yaw
        bodyRotationController.xHeadRot = rot.pitch
        bodyRotationController.yHeadRot = rot.yaw
        yHeadRot = rot.yaw
        yBodyRot = rot.yaw
    }

    val modeledEntity: ModeledEntity? get() = ModelEngineAPI.getModeledEntity(dummy.uuid)
    val activeModel: ActiveModel? get() = modeledEntity?.getModel(modelId)?.orElse(null)

    init {
        // Tie ModelEngine's visibility to our internal viewers set
        dummy.data.tracked.playerPredicate = Predicate { p -> _viewers.contains(p.uniqueId) }

        val me = ModelEngineAPI.createModeledEntity(dummy)
        val model = ModelEngineAPI.createActiveModel(modelId)
        me.addModel(model, true)

        InteractableModule.register(this)
        refreshViewers()
    }

    fun updateVisibility(player: Player) {
        if (audience.canBeApplied(player)) {
            _viewers.add(player.uniqueId)
        } else {
            _viewers.remove(player.uniqueId)
        }
    }

    fun removeViewer(player: Player) {
        _viewers.remove(player.uniqueId)
    }

    private fun refreshViewers() {
        for (player in Bukkit.getOnlinePlayers()) {
            updateVisibility(player)
        }
    }

    fun setSkin(profile: PlayerProfile) {
        activeModel?.bones?.values?.forEach { bone ->
            bone.getBoneBehavior(BoneBehaviorTypes.PLAYER_LIMB).ifPresent {
                (it as PlayerLimb).setTexture(profile)
            }
        }
    }

    fun setTint(tint: Color) {
        activeModel?.defaultTint = tint
    }

    fun playAnimation(id: String, lerpIn: Double = 0.0, lerpOut: Double = 0.0, speed: Double = 1.0) {
        activeModel?.animationHandler?.playAnimation(id, lerpIn, lerpOut, speed, true)
    }

    override fun destroy() {
        activeModel?.let {
            it.destroy()
            it.isRemoved = true
        }
        dummy.isRemoved = true
        InteractableModule.unregister(this)
        _viewers.clear()
    }
}
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
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull

class MEGInteractable(
    override val location: Location,
    val modelId: String,
    audience: AquaticAudience,
    override val onInteract: (InteractableInteractEvent) -> Unit,
) : Interactable() {

    override val viewers: MutableSet<Player> = mutableSetOf()

    override var audience: AquaticAudience = audience
        set(value) {
            field = value
            for (player in viewers.toList()) {
                if (!field.canBeApplied(player)) {
                    removeViewer(player)
                }
            }
            for (player in Bukkit.getOnlinePlayers()) {
                if (viewers.contains(player)) continue
                if (!field.canBeApplied(player)) continue
                addViewer(player)
            }
        }

    val dummy = MEGInteractableDummy(this).apply {
        location = this@MEGInteractable.location
        bodyRotationController.yBodyRot = location.yaw
        bodyRotationController.xHeadRot = location.pitch
        bodyRotationController.yHeadRot = location.yaw
        yHeadRot = location.yaw
        yBodyRot = location.yaw
    }

    val modeledEntity: ModeledEntity?
        get() {
            return ModelEngineAPI.getModeledEntity(dummy.uuid)
        }
    val activeModel: ActiveModel?
        get() {
            return modeledEntity?.getModel(modelId)?.getOrNull()
        }

    fun setSkin(player: Player) {
        setSkin(player.playerProfile)
    }

    fun setSkin(playerProfile: PlayerProfile) {
        activeModel?.apply {
            for (value in bones.values) {
                value.getBoneBehavior(BoneBehaviorTypes.PLAYER_LIMB).ifPresent {
                    (it as PlayerLimb).setTexture(playerProfile)
                }
            }
        }
    }

    fun setTint(tint: Color) {
        activeModel?.apply {
            this.defaultTint = tint
        }
    }

    init {
        this.audience = audience
        dummy.data.tracked.playerPredicate = Predicate { p -> viewers.contains(p) }
        val modeledEntity = ModelEngineAPI.createModeledEntity(dummy)
        val activeModel = ModelEngineAPI.createActiveModel(modelId)
        synchronized(InteractableModule.megInteractables) {
            InteractableModule.megInteractables += this
        }
        modeledEntity.addModel(activeModel, true)
    }


    override fun addViewer(player: Player) {
        viewers.add(player)
    }

    override fun removeViewer(player: Player) {
        viewers.remove(player)
    }


    override fun destroy() {
        this.activeModel?.destroy()
        this.activeModel?.isRemoved = true
        dummy.isRemoved = true
        synchronized(InteractableModule.megInteractables) {
            InteractableModule.megInteractables -= this
        }
        viewers.clear()
    }

    fun playAnimation(id: String, lerpIn: Double = .0, lerpOut: Double = .0, speed: Double = 1.0) {
        activeModel?.animationHandler?.playAnimation(id, lerpIn, lerpOut, speed, true)
    }

    override fun updateViewers() {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (audience.canBeApplied(player)) {
                addViewer(player)
            }
        }
    }
}
package gg.aquatic.waves.audience

import org.bukkit.entity.Player

private class CombinedAudience(
    val first: AquaticAudience,
    val second: AquaticAudience,
    val combineOr: Boolean
) : AquaticAudience {
    override fun canBeApplied(player: Player): Boolean {
        return if (combineOr) first.canBeApplied(player) || second.canBeApplied(player)
        else first.canBeApplied(player) && second.canBeApplied(player)
    }
}
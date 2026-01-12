package gg.aquatic.waves.clientside.entity

import gg.aquatic.waves.clientside.EntityBased
import org.bukkit.entity.Player

class FakeEntityInteractEvent(
    val fakeEntity: EntityBased,
    val player: Player,
    val isLeftClick: Boolean
)
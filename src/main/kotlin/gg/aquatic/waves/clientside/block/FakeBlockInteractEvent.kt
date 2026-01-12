package gg.aquatic.waves.clientside.block

import org.bukkit.entity.Player

class FakeBlockInteractEvent(
    val fakeBlock: FakeBlock,
    val player: Player,
    val isLeftClick: Boolean
)
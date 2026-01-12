package gg.aquatic.waves.interactable

import org.bukkit.entity.Player

class InteractableInteractEvent(
    val interactable: Interactable,
    val player: Player,
    val isLeft: Boolean
)
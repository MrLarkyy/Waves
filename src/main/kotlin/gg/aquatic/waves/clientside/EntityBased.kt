package gg.aquatic.waves.clientside

import gg.aquatic.waves.clientside.entity.FakeEntityInteractEvent

interface EntityBased {

    val entityId: Int
    var onInteract: (FakeEntityInteractEvent) -> Unit
}
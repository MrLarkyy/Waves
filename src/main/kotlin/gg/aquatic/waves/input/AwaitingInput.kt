package gg.aquatic.waves.input

import kotlinx.coroutines.CancellableContinuation
import org.bukkit.entity.Player

class AwaitingInput(
    val player: Player,
    val continuation: CancellableContinuation<String?>,
    val handle: InputHandle
)
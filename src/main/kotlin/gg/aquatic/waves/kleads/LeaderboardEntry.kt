package gg.aquatic.waves.kleads

import java.math.BigDecimal
import java.util.*

data class LeaderboardEntry(
    val uuid: UUID,
    val value: BigDecimal,
    val displayName: String,
    val rank: Long = -1
)
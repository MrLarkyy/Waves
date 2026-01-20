package gg.aquatic.waves.statistic.impl

import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.waves.statistic.ListenerStatisticType
import gg.aquatic.waves.statistic.StatisticAddEvent
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

object DeathStatistic: ListenerStatisticType<Player>() {
    override val arguments: Collection<ObjectArgument<*>> = listOf()

    private var listener: Listener? = null
    override fun createListener() = listen<PlayerDeathEvent> {
        val player = it.entity

        for (statisticHandle in handles) {
            val event = StatisticAddEvent(this, 1, player)
            statisticHandle.consumer(event)
        }
    }
}
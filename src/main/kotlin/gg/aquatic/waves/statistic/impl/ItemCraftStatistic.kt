package gg.aquatic.waves.statistic.impl

import gg.aquatic.common.event
import gg.aquatic.execute.argument.ObjectArgument
import gg.aquatic.waves.statistic.ListenerStatisticType
import gg.aquatic.waves.statistic.StatisticAddEvent
import gg.aquatic.waves.statistic.StatisticType
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent

object ItemCraftStatistic: ListenerStatisticType<Player>() {
    override val arguments: Collection<ObjectArgument<*>> = listOf()

    override fun createListener() = listen<CraftItemEvent> {
        val player = it.whoClicked as? Player ?: return@listen

        for (statisticHandle in handles) {
            val event = StatisticAddEvent(this, 1, player)
            statisticHandle.consumer(event)
        }
    }
}
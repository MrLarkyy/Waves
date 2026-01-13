package gg.aquatic.waves.statistic.impl

import gg.aquatic.common.event
import gg.aquatic.execute.argument.ObjectArgument
import gg.aquatic.waves.statistic.StatisticAddEvent
import gg.aquatic.waves.statistic.StatisticType
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent

object ItemCraftStatistic: StatisticType<Player>() {
    override val arguments: Collection<ObjectArgument<*>> = listOf()

    private var listener: Listener? = null

    override fun initialize() {
        listener = event<CraftItemEvent>(ignoredCancelled = true) {
            val player = it.whoClicked as? Player ?: return@event

            for (statisticHandle in handles) {
                val event = StatisticAddEvent(this, 1, player)
                statisticHandle.consumer(event)
            }
        }
    }

    override fun terminate() {
        listener?.let { HandlerList.unregisterAll(it) }
        listener = null
    }
}
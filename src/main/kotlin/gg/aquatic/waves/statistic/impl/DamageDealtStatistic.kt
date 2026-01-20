package gg.aquatic.waves.statistic.impl

import gg.aquatic.common.event
import gg.aquatic.execute.argument.ObjectArgument
import gg.aquatic.execute.argument.impl.PrimitiveObjectArgument
import gg.aquatic.waves.statistic.ListenerStatisticType
import gg.aquatic.waves.statistic.StatisticAddEvent
import gg.aquatic.waves.statistic.StatisticType
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

object DamageDealtStatistic: ListenerStatisticType<Player>() {
    override val arguments: Collection<ObjectArgument<*>> = listOf(
        PrimitiveObjectArgument("types", ArrayList<String>(), true)
    )

    override fun createListener() = listen<EntityDamageByEntityEvent> {
        val player = it.damager as? Player ?: return@listen

        for (statisticHandle in handles) {
            val args = statisticHandle.args
            val types = args.stringCollection("types") ?: listOf()

            if ("ALL" !in types && it.entity.type.name.uppercase() !in types) {
                continue
            }
            val event = StatisticAddEvent(this, it.damage, player)
            statisticHandle.consumer(event)
        }
    }
}
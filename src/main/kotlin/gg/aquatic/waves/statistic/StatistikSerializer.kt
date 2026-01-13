package gg.aquatic.waves.statistic

import gg.aquatic.execute.argument.ArgumentSerializer
import gg.aquatic.execute.argument.ObjectArguments
import org.bukkit.configuration.ConfigurationSection

object StatistikSerializer {

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> fromSection(
        configurationSection: ConfigurationSection,
        noinline consumer: (StatisticAddEvent<T>) -> Unit
    ): StatisticHandle<T>? {
        val typeId = configurationSection.getString("type") ?: return null
        val registry = StatisticType.REGISTRY[T::class.java] ?: return null
        val type = registry[typeId] as? StatisticType<T> ?: return null
        val loadedArgs = ArgumentSerializer.load(configurationSection, type.arguments)
        val args = ObjectArguments(loadedArgs)

        return StatisticHandle(type, args, consumer)
    }

    inline fun <reified T> fromSections(
        configurationSections: List<ConfigurationSection>,
        noinline consumer: (StatisticAddEvent<T>) -> Unit
    ): List<StatisticHandle<T>> {
        return configurationSections.mapNotNull { fromSection<T>(it, consumer) }
    }

}
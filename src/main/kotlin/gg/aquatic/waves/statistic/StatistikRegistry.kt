package gg.aquatic.waves.statistic

import gg.aquatic.kregistry.*

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> MutableRegistry<Class<*>, FrozenRegistry<String, StatisticType<*>>>.registerStatistic(
    id: String,
    action: StatisticType<T>
) {
    register<String, StatisticType<*>, StatisticType<T>>(id, action)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> MutableRegistry<Class<*>, FrozenRegistry<String, StatisticType<*>>>.registerStatistics(
    map: Map<String, StatisticType<T>>
) {
    register<String, StatisticType<*>, StatisticType<T>>(map)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> TypedRegistry<String, StatisticType<*>>.getStatistics(): Map<String, StatisticType<T>> {
    return this[T::class.java]?.getAll() as? Map<String, StatisticType<T>> ?: emptyMap()
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> TypedRegistry<String, StatisticType<*>>.getHierarchical(id: String): StatisticType<T>? {
    return this.getHierarchical<String, StatisticType<*>, StatisticType<T>>(id)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> TypedRegistry<String, StatisticType<*>>.getAllHierarchical(): Map<String, StatisticType<T>> {
    return this.getAllHierarchical<String, StatisticType<*>, StatisticType<T>>()
}

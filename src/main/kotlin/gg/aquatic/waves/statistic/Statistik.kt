package gg.aquatic.waves.statistic

import gg.aquatic.kregistry.FrozenRegistry
import gg.aquatic.kregistry.MutableRegistry
import gg.aquatic.kregistry.Registry

fun initializeStatistik() {
    val registry = MutableRegistry<Class<*>, FrozenRegistry<String, StatisticType<*>>>()

    Registry.update { registerRegistry(StatisticType.REGISTRY_KEY, registry.freeze()) }
}
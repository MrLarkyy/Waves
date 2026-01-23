package gg.aquatic.waves.kmetrics

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import gg.aquatic.common.ticker.GlobalTicker
import gg.aquatic.statistik.StatisticAddEvent
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object KMetricsManager {

    private lateinit var handler: KMetricsDBHandler
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val updateBatch = ConcurrentHashMap<String, ConcurrentHashMap<UUID, Pair<BigDecimal, String>>>()

    private val valueCache: Cache<String, BigDecimal> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build()

    fun initialize(db: Database) {
        this.handler = KMetricsDBHandler(db)
        transaction(db) { SchemaUtils.create(KMetricsTable) }

        GlobalTicker.runRepeatFixedRate(30_000L) {
            flushAll()
        }

        GlobalTicker.runRepeatFixedRate(43_200_000L) {
            performCleanup()
        }
    }

    fun <T : Any> registerMetric(
        id: String,
        statisticHandleFactory: ((StatisticAddEvent<T>) -> Unit) -> gg.aquatic.statistik.StatisticHandle<T>,
        binderToUuid: (T) -> UUID,
        nameProvider: (T) -> String
    ) {
        statisticHandleFactory { e ->
            val uuid = binderToUuid(e.binder)
            val name = nameProvider(e.binder)
            val increase = when (val amount = e.increasedAmount) {
                is BigDecimal -> amount
                else -> BigDecimal.valueOf(amount.toDouble())
            }.setScale(2, RoundingMode.HALF_UP)

            val metricBatch = updateBatch.getOrPut(id) { ConcurrentHashMap() }
            metricBatch.compute(uuid) { _, existing ->
                if (existing == null) increase to name
                else (existing.first + increase) to name
            }

            PeriodType.entries.forEach { type ->
                val cacheKey = "${id}_${type.name}_$uuid"
                val current = valueCache.getIfPresent(cacheKey)
                if (current != null) {
                    valueCache.put(cacheKey, current + increase)
                }
            }
        }
    }

    suspend fun getMetricValue(metricId: String, uuid: UUID, type: PeriodType): BigDecimal {
        val cacheKey = "${metricId}_${type.name}_$uuid"

        return valueCache.getIfPresent(cacheKey) ?: run {
            val dbValue = handler.getValue(metricId, uuid, type, type.getPeriodId())
            valueCache.put(cacheKey, dbValue)
            dbValue
        }
    }

    private suspend fun performCleanup() {
        try {
            // Keep the last 30 daily entries
            handler.cleanup(PeriodType.DAILY, 30)

            // Keep the last 24 weekly entries (~6 months)
            handler.cleanup(PeriodType.WEEKLY, 24)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun flushAll() {
        if (updateBatch.isEmpty()) return

        val toProcess = HashMap<String, Map<UUID, Pair<BigDecimal, String>>>()
        val iterator = updateBatch.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            toProcess[entry.key] = HashMap(entry.value)
            iterator.remove()
        }

        try {
            handler.batchUpdate(toProcess)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        runBlocking { flushAll() }
        valueCache.invalidateAll()
        scope.cancel()
    }
}

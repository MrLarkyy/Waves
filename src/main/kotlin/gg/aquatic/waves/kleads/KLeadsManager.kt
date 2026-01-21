package gg.aquatic.waves.kleads

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.common.ticker.Ticker
import gg.aquatic.statistik.StatisticAddEvent
import gg.aquatic.statistik.StatisticHandle
import gg.aquatic.waves.kmetrics.PeriodType
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object KLeadsManager {

    private lateinit var handler: KLeadsDBHandler
    private val leaderboards = ConcurrentHashMap<String, Leaderboard>()

    private var refreshTick = 0

    fun initialize(db: Database) {
        this.handler = KLeadsDBHandler(db)

        Ticker {
            refreshTick++
            if (refreshTick >= 60 * 30) { // Refresh every 30 minutes
                refreshAllLeaderboards()
                refreshTick = 0
            }
        }.register()
    }

    /**
     * Registers a leaderboard that reads from KMetrics.
     * @param metricId The ID of the metric in KMetrics
     * @param type The period (DAILY, WEEKLY, etc.)
     * @param topLimit How many top players to cache
     */
    fun registerLeaderboard(
        metricId: String,
        type: PeriodType,
        topLimit: Int = 10
    ): Leaderboard {
        val id = "${metricId}_${type.name}"
        val lb = Leaderboard(metricId, type, topLimit)
        leaderboards[id] = lb
        return lb
    }

    fun getLeaderboard(metricId: String, type: PeriodType): Leaderboard? {
        return leaderboards["${metricId}_${type.name}"]
    }

    private suspend fun refreshAllLeaderboards() {
        leaderboards.values.forEach { it.refresh() }
    }

    class Leaderboard(val metricId: String, val type: PeriodType, val topLimit: Int) {
        private var cachedEntries = listOf<LeaderboardEntry>()

        fun getTop(): List<LeaderboardEntry> = cachedEntries

        suspend fun getRank(uuid: UUID): LeaderboardEntry? {
            return handler.getRank(metricId, type, type.getPeriodId(), uuid)
        }

        suspend fun getRankByName(name: String): LeaderboardEntry? {
            return handler.getRankByName(metricId, type, type.getPeriodId(), name)
        }

        internal fun updateCache(entries: List<LeaderboardEntry>) {
            this.cachedEntries = entries
        }

        suspend fun refresh() {
            val entries = handler.getTop(metricId, type, type.getPeriodId(), topLimit)
            updateCache(entries)
        }
    }
}

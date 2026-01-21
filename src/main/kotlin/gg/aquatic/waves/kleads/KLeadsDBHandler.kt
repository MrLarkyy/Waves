package gg.aquatic.waves.kleads

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.waves.kmetrics.KMetricsTable
import gg.aquatic.waves.kmetrics.PeriodType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class KLeadsDBHandler(private val database: Database) {

    suspend fun getTop(metricId: String, type: PeriodType, pId: String, limit: Int): List<LeaderboardEntry> =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            val rowNum = RowNumber().over()
                .orderBy(KMetricsTable.value to SortOrder.DESC)
                .alias("rn")

            KMetricsTable
                .select(KMetricsTable.binderUuid, KMetricsTable.value, KMetricsTable.displayName, rowNum)
                .where {
                    (KMetricsTable.metricId eq metricId) and
                            (KMetricsTable.periodType eq type) and
                            (KMetricsTable.periodId eq pId)
                }
                .orderBy(KMetricsTable.value to SortOrder.DESC)
                .limit(limit)
                .map {
                    LeaderboardEntry(
                        it[KMetricsTable.binderUuid],
                        it[KMetricsTable.value],
                        it[KMetricsTable.displayName],
                        it[rowNum]
                    )
                }
        }

    suspend fun getRank(metricId: String, type: PeriodType, pId: String, uuid: UUID): LeaderboardEntry? =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            val rowNum = RowNumber().over()
                .orderBy(KMetricsTable.value to SortOrder.DESC)
                .alias("rn")

            val subquery = KMetricsTable
                .select(KMetricsTable.binderUuid, KMetricsTable.value, KMetricsTable.displayName, rowNum)
                .where {
                    (KMetricsTable.metricId eq metricId) and
                            (KMetricsTable.periodType eq type) and
                            (KMetricsTable.periodId eq pId)
                }
                .alias("ranked")

            val rnAlias = subquery[rowNum]

            subquery
                .select(subquery[KMetricsTable.binderUuid], subquery[KMetricsTable.value], subquery[KMetricsTable.displayName], rnAlias)
                .where { subquery[KMetricsTable.binderUuid] eq uuid }
                .map {
                    LeaderboardEntry(
                        it[subquery[KMetricsTable.binderUuid]],
                        it[subquery[KMetricsTable.value]],
                        it[subquery[KMetricsTable.displayName]],
                        it[rnAlias]
                    )
                }
                .singleOrNull()
        }

    suspend fun getRankByName(metricId: String, type: PeriodType, pId: String, name: String): LeaderboardEntry? =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            val rowNum = RowNumber().over()
                .orderBy(KMetricsTable.value to SortOrder.DESC)
                .alias("rn")

            val subquery = KMetricsTable
                .select(KMetricsTable.binderUuid, KMetricsTable.value, KMetricsTable.displayName, rowNum)
                .where {
                    (KMetricsTable.metricId eq metricId) and
                            (KMetricsTable.periodType eq type) and
                            (KMetricsTable.periodId eq pId)
                }
                .alias("ranked")

            val rnAlias = subquery[rowNum]

            subquery
                .select(subquery[KMetricsTable.binderUuid], subquery[KMetricsTable.value], subquery[KMetricsTable.displayName], rnAlias)
                .where { subquery[KMetricsTable.displayName] eq name }
                .firstOrNull()
                ?.let {
                    LeaderboardEntry(
                        it[subquery[KMetricsTable.binderUuid]],
                        it[subquery[KMetricsTable.value]],
                        it[subquery[KMetricsTable.displayName]],
                        it[rnAlias]
                    )
                }
        }
}
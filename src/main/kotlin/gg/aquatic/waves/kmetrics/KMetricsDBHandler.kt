package gg.aquatic.waves.kmetrics

import gg.aquatic.common.coroutine.VirtualsCtx
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class KMetricsDBHandler(private val database: Database) {

    suspend fun batchUpdate(updates: Map<String, Map<UUID, Pair<BigDecimal, String>>>) =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            if (updates.isEmpty()) return@newSuspendedTransaction

            val now = LocalDateTime.now()

            updates.forEach { (metricId, playerMap) ->
                playerMap.forEach { (uuid, data) ->
                    val (increase, name) = data

                    for (type in PeriodType.entries) {
                        val pId = type.getPeriodId(now)

                        KMetricsTable.upsert(onUpdate = {
                            it[KMetricsTable.value] = KMetricsTable.value + increase; it[KMetricsTable.displayName] =
                            name
                        }) {
                            it[KMetricsTable.metricId] = metricId
                            it[KMetricsTable.binderUuid] = uuid
                            it[KMetricsTable.periodType] = type
                            it[KMetricsTable.periodId] = pId
                            it[KMetricsTable.displayName] = name
                            it[KMetricsTable.value] = increase
                        }
                    }
                }
            }
        }

    suspend fun cleanup(type: PeriodType, amountToKeep: Int) =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            val now = LocalDateTime.now()
            val idsToKeep = mutableListOf<String>()

            for (i in 0 until amountToKeep) {
                val time = when (type) {
                    PeriodType.DAILY -> now.minusDays(i.toLong())
                    PeriodType.WEEKLY -> now.minusWeeks(i.toLong())
                    PeriodType.MONTHLY -> now.minusMonths(i.toLong())
                    PeriodType.ALLTIME -> now
                }
                idsToKeep.add(type.getPeriodId(time))
            }

            KMetricsTable.deleteWhere {
                (KMetricsTable.periodType eq type) and
                        (KMetricsTable.periodId notInList idsToKeep)
            }
        }

    suspend fun getValue(metricId: String, uuid: UUID, type: PeriodType, pId: String): BigDecimal =
        newSuspendedTransaction(db = database, context = VirtualsCtx) {
            KMetricsTable.select(KMetricsTable.value)
                .where {
                    (KMetricsTable.metricId eq metricId) and
                            (KMetricsTable.binderUuid eq uuid) and
                            (KMetricsTable.periodType eq type) and
                            (KMetricsTable.periodId eq pId)
                }
                .singleOrNull()?.get(KMetricsTable.value) ?: BigDecimal.ZERO
        }
}

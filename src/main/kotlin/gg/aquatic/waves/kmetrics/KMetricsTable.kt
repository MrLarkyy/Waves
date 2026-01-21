package gg.aquatic.waves.kmetrics

import org.jetbrains.exposed.sql.Table

object KMetricsTable : Table("kmetrics_entries") {
    val metricId = varchar("metric_id", 64)
    val binderUuid = uuid("binder_uuid")
    val periodType = enumerationByName("period_type", 16, PeriodType::class)
    val periodId = varchar("period_id", 32)
    val value = decimal("value", 36, 2)
    val displayName = varchar("display_name", 128)

    override val primaryKey = PrimaryKey(metricId, binderUuid, periodType, periodId)
}

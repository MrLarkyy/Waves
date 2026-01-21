package gg.aquatic.waves.kmetrics

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

enum class PeriodType {
    DAILY, WEEKLY, MONTHLY, ALLTIME;

    fun getPeriodId(time: LocalDateTime = LocalDateTime.now()): String {
        return when (this) {
            DAILY -> time.format(DateTimeFormatter.ofPattern("yyyy-DDD"))
            WEEKLY -> "${time.year}-W${time.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            MONTHLY -> "${time.year}-${time.monthValue}"
            ALLTIME -> "GLOBAL"
        }
    }
}

package gg.aquatic.waves.kmetrics

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KMetricsTest {

    private lateinit var db: Database
    private lateinit var handler: KMetricsDBHandler

    @BeforeTest
    fun setup() {
        // Initialize in-memory H2 database for testing
        db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", driver = "org.h2.Driver")
        handler = KMetricsDBHandler(db)
        
        transaction(db) {
            SchemaUtils.create(KMetricsTable)
        }
    }

    @Test
    fun `test batch update fans out and increments correctly`() = runBlocking {
        val metricId = "test_metric"
        val uuid = UUID.randomUUID()
        val name = "TestPlayer"
        
        // 1. Initial update: +10.00
        val update1 = mapOf(
            metricId to mapOf(uuid to (BigDecimal("10.00") to name))
        )
        handler.batchUpdate(update1)

        // Verify Daily value is 10
        val dailyValue1 = handler.getValue(metricId, uuid, PeriodType.DAILY, PeriodType.DAILY.getPeriodId())
        assertEquals(BigDecimal("10.00"), dailyValue1)

        // 2. Second update: +5.50
        val update2 = mapOf(
            metricId to mapOf(uuid to (BigDecimal("5.50") to name))
        )
        handler.batchUpdate(update2)

        // Verify Daily value is now 15.50 (Cumulative check)
        val dailyValue2 = handler.getValue(metricId, uuid, PeriodType.DAILY, PeriodType.DAILY.getPeriodId())
        assertEquals(BigDecimal("15.50"), dailyValue2)

        // Verify All-time value is also updated (Fan-out check)
        val allTimeValue = handler.getValue(metricId, uuid, PeriodType.ALLTIME, PeriodType.ALLTIME.getPeriodId())
        assertEquals(BigDecimal("15.50"), allTimeValue)
    }

    @Test
    fun `test cleanup removes old periods`() = runBlocking {
        // This is a simplified test for cleanup. 
        // In a real scenario, you'd mock the LocalDateTime inside the handler,
        // but we can verify it doesn't delete the current period.
        
        val metricId = "cleanup_test"
        val uuid = UUID.randomUUID()
        val update = mapOf(metricId to mapOf(uuid to (BigDecimal("100") to "Player")))
        
        handler.batchUpdate(update)
        
        // Perform cleanup keeping only 1 day
        handler.cleanup(PeriodType.DAILY, 1)
        
        // Current day should still exist
        val value = handler.getValue(metricId, uuid, PeriodType.DAILY, PeriodType.DAILY.getPeriodId())
        assertEquals(BigDecimal("100.00"), value)
    }
}

package gg.aquatic.waves.currency

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gg.aquatic.waves.currency.db.BalancesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object CurrencyDatabaseFactory {

    fun init(url: String, driver: String, user: String, pass: String): Database {
        val config = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            password = pass

            // Connection Timeout & Reconnection Settings
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // If DB is down, how long to wait for a connection before throwing exception (3 seconds)
            connectionTimeout = 3000

            // Max time a connection can be idle (10 minutes)
            idleTimeout = 600000

            // Max lifetime of a connection (30 minutes) - helps prevent "stale" connections
            maxLifetime = 1800000

            // Optimization for MySQL/MariaDB
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        val dataSource = HikariDataSource(config)
        val db = Database.Companion.connect(dataSource)

        // Initialize Tables
        transaction(db) {
            SchemaUtils.create(BalancesTable)
        }

        return db
    }
}
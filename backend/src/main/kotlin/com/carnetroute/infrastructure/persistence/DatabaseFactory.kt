package com.carnetroute.infrastructure.persistence

import com.carnetroute.infrastructure.persistence.tables.FuelPricesTable
import com.carnetroute.infrastructure.persistence.tables.JourneyHistoriesTable
import com.carnetroute.infrastructure.persistence.tables.SimulationsTable
import com.carnetroute.infrastructure.persistence.tables.UsersTable
import com.carnetroute.infrastructure.persistence.tables.VehiclesTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class DatabaseFactory(private val config: ApplicationConfig) {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val dbUrl = config.property("app.database.url").getString()
        val dbUser = config.property("app.database.user").getString()
        val dbPassword = config.property("app.database.password").getString()
        val maxPoolSize = config.propertyOrNull("app.database.maxPoolSize")
            ?.getString()?.toIntOrNull() ?: 10

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            initializationFailTimeout = -1  // retry indéfiniment si la DB n'est pas prête
            connectionTimeout = 30_000
            validate()
        }

        dataSource = HikariDataSource(hikariConfig)

        // Migrations Flyway (optionnel — ignoré si pas de scripts)
        runMigrations(dbUrl, dbUser, dbPassword)

        // Connexion Exposed
        Database.connect(dataSource)

        // Création/mise à jour des tables (mode dev)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                VehiclesTable,
                SimulationsTable,
                JourneyHistoriesTable,
                FuelPricesTable,
            )
        }

        logger.info("Base de données initialisée (pool: $maxPoolSize connexions, url: $dbUrl)")
    }

    private fun runMigrations(url: String, user: String, password: String) {
        try {
            Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate()
            logger.info("Migrations Flyway appliquées avec succès")
        } catch (e: Exception) {
            logger.warn("Flyway : ${e.message} — création via Exposed SchemaUtils")
        }
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}

/**
 * Lance un bloc dans une transaction Exposed suspendue sur le dispatcher IO.
 */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

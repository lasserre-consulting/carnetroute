package com.carnetroute.infrastructure.persistence.repositories

import com.carnetroute.domain.user.User
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.domain.user.vo.UserPreferences
import com.carnetroute.infrastructure.persistence.DatabaseFactory
import com.carnetroute.infrastructure.persistence.dbQuery
import com.carnetroute.infrastructure.persistence.tables.UsersTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class UserRepositoryImpl(
    @Suppress("UNUSED_PARAMETER") private val databaseFactory: DatabaseFactory,
) : UserRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun findById(id: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun save(user: User): User = dbQuery {
        UsersTable.insert {
            it[id] = user.id
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[name] = user.name
            it[preferences] = json.encodeToString(user.preferences)
            it[createdAt] = user.createdAt
            it[updatedAt] = user.updatedAt
        }
        user
    }

    override suspend fun update(user: User): User = dbQuery {
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[name] = user.name
            it[preferences] = json.encodeToString(user.preferences)
            it[updatedAt] = user.updatedAt
        }
        user
    }

    override suspend fun delete(id: String): Unit = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id }
    }

    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .count() > 0
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        name = this[UsersTable.name],
        preferences = runCatching {
            json.decodeFromString<UserPreferences>(this[UsersTable.preferences])
        }.getOrDefault(UserPreferences()),
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
    )
}

package com.carnetroute.infrastructure.persistence.tables

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255)
    val preferences = text("preferences") // JSON serialized UserPreferences
    val createdAt = varchar("created_at", 30)
    val updatedAt = varchar("updated_at", 30)
    override val primaryKey = PrimaryKey(id)
}

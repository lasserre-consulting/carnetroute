package com.carnetroute.domain.user

interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun save(user: User): User
    suspend fun update(user: User): User
    suspend fun delete(id: String)
    suspend fun existsByEmail(email: String): Boolean
}

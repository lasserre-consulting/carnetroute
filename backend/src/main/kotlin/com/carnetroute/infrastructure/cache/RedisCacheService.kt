package com.carnetroute.infrastructure.cache

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisCacheService(redisUrl: String) {
    private val pool: JedisPool

    init {
        // Parse URL format: redis://host:port
        val uri = java.net.URI(redisUrl)
        val config = JedisPoolConfig().apply {
            maxTotal = 20
            maxIdle = 10
            minIdle = 2
            testOnBorrow = true
        }
        pool = JedisPool(config, uri.host, uri.port)
    }

    fun get(key: String): String? = pool.resource.use { it.get(key) }

    fun set(key: String, value: String, ttlSeconds: Long = 3600) {
        pool.resource.use { it.setex(key, ttlSeconds, value) }
    }

    fun del(key: String) = pool.resource.use { it.del(key) }

    fun del(vararg keys: String) = pool.resource.use { it.del(*keys) }

    fun exists(key: String): Boolean = pool.resource.use { it.exists(key) }

    inline fun <reified T> getOrSet(
        key: String,
        ttlSeconds: Long = 3600,
        serializer: (T) -> String,
        deserializer: (String) -> T,
        compute: () -> T
    ): T {
        val cached = get(key)
        if (cached != null) return deserializer(cached)
        val value = compute()
        set(key, serializer(value), ttlSeconds)
        return value
    }

    fun close() = pool.close()
}

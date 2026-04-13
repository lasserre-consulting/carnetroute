package com.carnetroute.domain.port

interface GeocodingPort {
    suspend fun autocomplete(query: String, limit: Int = 7): String
    suspend fun reverse(lat: Double, lng: Double): String
}

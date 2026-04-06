package com.carnetroute.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class GeocodingService {

    private val logger = LoggerFactory.getLogger(GeocodingService::class.java)
    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Proxy autocomplete requests to api-adresse.data.gouv.fr
     * This solves CORS issues when the frontend calls from the browser
     */
    suspend fun autocomplete(query: String, limit: Int = 7): String {
        return try {
            val response: HttpResponse = client.get("https://api-adresse.data.gouv.fr/search/") {
                parameter("q", query)
                parameter("limit", limit)
                parameter("autocomplete", 1)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Geocoding API error: ${e.message}")
            """{"type":"FeatureCollection","features":[]}"""
        }
    }

    /**
     * Reverse geocode coordinates to an address
     */
    suspend fun reverse(lat: Double, lng: Double): String {
        return try {
            val response: HttpResponse = client.get("https://api-adresse.data.gouv.fr/reverse/") {
                parameter("lat", lat)
                parameter("lon", lng)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Reverse geocoding error: ${e.message}")
            """{"type":"FeatureCollection","features":[]}"""
        }
    }
}

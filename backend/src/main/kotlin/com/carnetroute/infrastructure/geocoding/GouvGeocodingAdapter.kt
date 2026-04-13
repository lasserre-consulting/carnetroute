package com.carnetroute.infrastructure.geocoding

import com.carnetroute.domain.port.GeocodingPort
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

/**
 * Adaptateur géocodage — proxy vers api-adresse.data.gouv.fr.
 * Résout les problèmes CORS du frontend sans exposer l'API externe directement.
 */
class GouvGeocodingAdapter : GeocodingPort {

    private val logger = LoggerFactory.getLogger(GouvGeocodingAdapter::class.java)
    private val client = HttpClient(CIO)

    override suspend fun autocomplete(query: String, limit: Int): String {
        return try {
            val response: io.ktor.client.statement.HttpResponse = client.get("https://api-adresse.data.gouv.fr/search/") {
                parameter("q", query)
                parameter("limit", limit)
                parameter("autocomplete", 1)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Erreur API géocodage : ${e.message}")
            EMPTY_FEATURE_COLLECTION
        }
    }

    override suspend fun reverse(lat: Double, lng: Double): String {
        return try {
            val response: io.ktor.client.statement.HttpResponse = client.get("https://api-adresse.data.gouv.fr/reverse/") {
                parameter("lat", lat)
                parameter("lon", lng)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Erreur géocodage inverse : ${e.message}")
            EMPTY_FEATURE_COLLECTION
        }
    }

    companion object {
        private const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
    }
}

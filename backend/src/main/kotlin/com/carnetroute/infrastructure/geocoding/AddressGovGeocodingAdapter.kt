package com.carnetroute.infrastructure.geocoding

import com.carnetroute.application.geocoding.AddressSuggestion
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AddressGovGeocodingAdapter : GeocodingPort {

    private val logger = LoggerFactory.getLogger(AddressGovGeocodingAdapter::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 4_000
        }
    }

    override suspend fun autocomplete(query: String, limit: Int): List<AddressSuggestion> {
        if (query.isBlank()) return emptyList()
        return try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "https://api-adresse.data.gouv.fr/search/?q=$encoded&limit=$limit"
            val response = client.get(url)

            if (!response.status.isSuccess()) {
                logger.warn("api-adresse.data.gouv.fr returned HTTP ${response.status.value} for query: $query")
                return emptyList()
            }

            parseGeoJsonResponse(response.bodyAsText())
        } catch (e: Exception) {
            logger.error("Geocoding request failed for query '$query': ${e.message}")
            emptyList()
        }
    }

    private fun parseGeoJsonResponse(body: String): List<AddressSuggestion> {
        val root = Json.parseToJsonElement(body).jsonObject
        val features = root["features"]?.jsonArray ?: return emptyList()

        return features.mapNotNull { element ->
            try {
                val feature = element.jsonObject
                val properties = feature["properties"]?.jsonObject ?: return@mapNotNull null
                val geometry = feature["geometry"]?.jsonObject ?: return@mapNotNull null
                val coordinates = geometry["coordinates"]?.jsonArray ?: return@mapNotNull null

                val label = properties["label"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val lng = coordinates[0].jsonPrimitive.double
                val lat = coordinates[1].jsonPrimitive.double
                val city = properties["city"]?.jsonPrimitive?.content ?: ""
                val postcode = properties["postcode"]?.jsonPrimitive?.content ?: ""

                AddressSuggestion(
                    label = label,
                    lat = lat,
                    lng = lng,
                    city = city,
                    postcode = postcode
                )
            } catch (e: Exception) {
                logger.warn("Failed to parse feature: ${e.message}")
                null
            }
        }
    }
}

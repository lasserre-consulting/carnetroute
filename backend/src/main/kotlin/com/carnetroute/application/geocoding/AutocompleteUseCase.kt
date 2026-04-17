package com.carnetroute.application.geocoding

import com.carnetroute.infrastructure.geocoding.GeocodingPort

data class AddressSuggestion(
    val label: String,
    val lat: Double,
    val lng: Double,
    val city: String = "",
    val postcode: String = ""
)

class AutocompleteUseCase(
    private val geocodingPort: GeocodingPort
) {
    suspend fun execute(query: String, limit: Int = 5): List<AddressSuggestion> {
        if (query.length < 3) return emptyList()
        return geocodingPort.autocomplete(query, limit)
    }
}

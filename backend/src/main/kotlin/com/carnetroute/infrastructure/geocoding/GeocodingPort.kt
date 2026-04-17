package com.carnetroute.infrastructure.geocoding

import com.carnetroute.application.geocoding.AddressSuggestion

interface GeocodingPort {
    suspend fun autocomplete(query: String, limit: Int = 5): List<AddressSuggestion>
}

package com.carnetroute.interfaces.http

import com.carnetroute.application.fuelprice.GetFuelPricesUseCase
import com.carnetroute.domain.fuelprice.FuelPrice
import com.carnetroute.domain.vehicle.vo.FuelType
import com.carnetroute.interfaces.http.dto.FuelPriceResponse
import com.carnetroute.interfaces.http.dto.FuelPricesResponse
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.datetime.Clock
fun Route.configureFuelPriceRoutes(
    getFuelPricesUseCase: GetFuelPricesUseCase,
) {

    // GET /api/fuels — liste des carburants avec prix actuels
    get("/fuels") {
        val prices = getFuelPricesUseCase.execute()
        val now = Clock.System.now().toString()
        call.respond(
            FuelPricesResponse(
                prices = prices.map { it.toResponse() },
                updatedAt = prices.firstOrNull()?.updatedAt ?: now
            )
        )
    }

    // GET /api/prices/live — alias de /fuels avec timestamp supplémentaire
    get("/prices/live") {
        val prices = getFuelPricesUseCase.execute()
        val now = Clock.System.now().toString()
        val updatedAt = prices.firstOrNull()?.updatedAt ?: now
        call.respond(
            mapOf(
                "prices" to prices.map { it.toResponse() },
                "updatedAt" to updatedAt,
                "timestamp" to now
            )
        )
    }
}

private fun FuelPrice.toResponse(): FuelPriceResponse {
    val fuelTypeEnum = runCatching { FuelType.fromString(fuelType) }.getOrNull()
    return FuelPriceResponse(
        fuelType = fuelType,
        displayName = fuelTypeEnum?.displayName ?: fuelType,
        pricePerUnit = pricePerUnit,
        unit = unit,
        defaultConsumption = fuelTypeEnum?.defaultConsumptionPer100km ?: 0.0,
        source = source,
        updatedAt = updatedAt
    )
}
